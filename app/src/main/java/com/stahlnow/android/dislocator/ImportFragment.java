package com.stahlnow.android.dislocator;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.OpenFileActivityBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;


public class ImportFragment extends Fragment implements
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks,
        PermissionResultListener
{

    private static final String TAG = ImportFragment.class.getSimpleName();
    private boolean mStorageAccessGranted = false;

    private ListView mFileListView;
    private ArrayAdapter<String> mFileListAdapter;


    private GoogleApiClient mGoogleApiClient;

    private static final int REQUEST_CODE_OPEN_FROM_DRIVE = 1;
    private static final int REQUEST_CODE_OPEN_FROM_SDCARD = 2;
    /**
     * Request code for auto Google Play Services error resolution.
     */
    protected static final int REQUEST_CODE_RESOLUTION = 1;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceBundle) {
        super.onActivityCreated(savedInstanceBundle);

        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
            // Create a GoogleApiClient instance
            mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                    .enableAutoManage(getActivity() /* FragmentActivity */,
                            1, /* client id */
                            this /* OnConnectionFailedListener */)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .build();
        }

    }


    @Override
    public void onPause() {
        super.onPause();
        // stop google services: this will disconnect and call @onConnectionSuspended
        mGoogleApiClient.stopAutoManage(getActivity());
    }

    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "GoogleApiClient connected.");
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended.");
    }


    /**
     * Handles failure to connect to the Google Play services client.
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "GoogleApiClient connection failed: " + connectionResult.toString());
        if (!connectionResult.hasResolution()) {
            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), connectionResult.getErrorCode(), 0).show();
            return;
        }
        try {
            connectionResult.startResolutionForResult(getActivity(), REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_import,container,false);

        Button btnImportMarkers = (Button)v.findViewById(R.id.btn_import_markers);
        btnImportMarkers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mGoogleApiClient.isConnected()) {
                    IntentSender intentSender = Drive.DriveApi
                            .newOpenFileActivityBuilder()
                            .setMimeType(new String[]{"application/vnd.google-earth.kml+xml", "application/vnd.google-earth.kmz"})
                            .build(mGoogleApiClient);
                    try {
                        getActivity().startIntentSenderForResult(intentSender, REQUEST_CODE_OPEN_FROM_DRIVE, null, 0, 0, 0);
                    } catch (IntentSender.SendIntentException e) {
                        Log.w(TAG, "Unable to send intent", e);
                    }
                }
            }
        });



        mFileListView = (ListView) v.findViewById(R.id.import_file_list);


        if (askForPermission()) // ask for storage permission
        {
            getFiles();
        }

        mFileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView v = (TextView)view.findViewById(R.id.list_item);
                String filename = v.getText().toString();
                Intent data = new Intent();
                data.putExtra("filename", filename);
                onActivityResult(REQUEST_CODE_OPEN_FROM_SDCARD, RESULT_OK, data);

            }
        });

        return v;
    }

    private void getFiles() {

        List<String> file_list = new ArrayList<String>();

        // add all files from internal storage
        File dir = new File(Environment.getExternalStorageDirectory(), "/Dislocator"); // "/" -> could be subdir instead of root

        // create directory, if it doesn't exist yet and copy assets

        boolean success = false;
        if (!dir.exists()) {
            success = dir.mkdirs();
        }
        if (success) {
            Log.e(TAG, "created directory: " + dir.getAbsoluteFile());
            // copy assets to internal storage files
            copyAssets("kml");
        } else {
            String state = Environment.getExternalStorageState();
            Log.e(TAG, "Error creating directory" + dir.getAbsolutePath());
            if (!Environment.MEDIA_MOUNTED.equals(state)){
                Log.e(TAG, "Error: external storage is unavailable");
            }
            if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                Log.e(TAG, "Error: external storage is read only.");
            }
        }


        File files[] = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".kml");
            }
        });

        if (files != null) {
            for (File f : files) {
                file_list.add(f.getName());
            }
        }

        mFileListAdapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item, file_list);
        mFileListView.setAdapter(mFileListAdapter);
    }

    private boolean askForPermission() {
        Activity activity = getActivity();
        if (activity != null && activity instanceof DislocatorActivity) {
            DislocatorActivity mainActivity = (DislocatorActivity) activity;
            mainActivity.setPermissionResultListener(this);

            // returns true if we have access
            mStorageAccessGranted = mainActivity.requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, DislocatorApplication.REQUEST_WRITE_EXTERNAL_STORAGE);
            return mStorageAccessGranted;
        }
        return false;
    }

    @Override
    public void onPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case DislocatorApplication.REQUEST_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onPermissionResult");
                    mStorageAccessGranted = true;

                    getFiles();
                    //mFileListAdapter.notifyDataSetChanged();
                }
                break;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, final Intent data) {

        final DislocatorActivity activity = (DislocatorActivity)getActivity();

        switch (requestCode) {

            case REQUEST_CODE_OPEN_FROM_DRIVE:

                if (resultCode == RESULT_OK) {
                    activity.driveId =
                            ((DriveId)(data.getParcelableExtra(
                                    OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID))).encodeToString();
                }
                break;

            case REQUEST_CODE_OPEN_FROM_SDCARD:
                if (resultCode == RESULT_OK) {
                    activity.sdCardFile = (String)(data.getStringExtra("filename"));
                }
            default:
                break;
        }


        LayoutInflater li = LayoutInflater.from(getActivity());
        View promptsView = li.inflate(R.layout.select_map_for_import_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder
                .setTitle(getString(R.string.select_map_for_import_dialog_title))
                .setNegativeButton(android.R.string.cancel, null) // dismisses by default);
                .setView(promptsView)
                .create();

        final Button btnImportToRemoteMap = (Button) promptsView.findViewById(R.id.btn_import_to_remote_map);
        final Button btnImportToLocalMap = (Button) promptsView.findViewById(R.id.btn_import_to_local_map);
        final Button btnImportToBothMaps = (Button) promptsView.findViewById(R.id.btn_import_to_both_maps);

        final AlertDialog alertDialog = alertDialogBuilder.create();

        btnImportToRemoteMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.import_to = DislocatorActivity.IMPORT_TO.REMOTE;
                openMap();
                alertDialog.cancel();
            }
        });

        btnImportToLocalMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.import_to = DislocatorActivity.IMPORT_TO.LOCAL;
                openMap();
                alertDialog.cancel();
            }
        });

        btnImportToBothMaps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.import_to = DislocatorActivity.IMPORT_TO.REMOTE_AND_LOCAL;
                openMap();
                alertDialog.cancel();
            }
        });

        alertDialog.show();



    }


    private void openMap() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction
                .replace(R.id.content,  new MapsFragment())
                .addToBackStack(getString(R.string.tag_fragment_import))
                .commit();
    }

    /// rest left for documentation


    private void copyAssets(String dir) {
        AssetManager assetManager = getActivity().getAssets();
        String[] files = null;
        try {
            files = assetManager.list(dir);
        } catch (IOException e) {
            Log.e("tag", "Failed to get asset file list.", e);
        }
        for(String filename : files) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = assetManager.open(dir + File.separatorChar + filename);
                File dst = new File(Environment.getExternalStorageDirectory(), "/Dislocator");
                File outFile = new File(dst, filename);
                out = new FileOutputStream(outFile);
                copyFileInputStream(in, out);
                in.close();
                in = null;
                out.flush();
                out.close();
                out = null;
            } catch(IOException e) {
                Log.e("tag", "Failed to copy asset file: " + filename, e);
            }
        }
    }

    private void copyFileInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    private static void copyFile(File src, File dst) throws IOException
    {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try
        {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        }
        finally
        {
            if (inChannel != null)
                inChannel.close();
            if (outChannel != null)
                outChannel.close();
        }
    }

}
