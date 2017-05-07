package com.stahlnow.android.dislocator;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.OpenFileActivityBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

import static android.app.Activity.RESULT_OK;


public class ImportFragment extends Fragment implements
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks
{

    private static final String TAG = ImportFragment.class.getSimpleName();

    private ListView mRemoteListView;
    private ListView mLocalListView;
    private ArrayAdapter<String> mRemoteListAdapter;
    private ArrayAdapter<String> mLocalListAdapter;


    private GoogleApiClient mGoogleApiClient;

    private static final int REQUEST_CODE_OPEN_REMOTE = 1;
    private static final int REQUEST_CODE_OPEN_LOCAL = 2;
    /**
     * Request code for auto Google Play Services error resolution.
     */
    protected static final int REQUEST_CODE_RESOLUTION = 1;
    private String mLocalDriveId = null;
    private String mRemoteDriveId = null;


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

        Button btnImportRemoteMarkers = (Button)v.findViewById(R.id.btn_import_remote_markers);
        btnImportRemoteMarkers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mGoogleApiClient.isConnected()) {
                    IntentSender intentSender = Drive.DriveApi
                            .newOpenFileActivityBuilder()
                            .setMimeType(new String[]{"application/vnd.google-earth.kml+xml", "application/vnd.google-earth.kmz"})
                            .build(mGoogleApiClient);
                    try {
                        getActivity().startIntentSenderForResult(intentSender, REQUEST_CODE_OPEN_REMOTE, null, 0, 0, 0);
                    } catch (IntentSender.SendIntentException e) {
                        Log.w(TAG, "Unable to send intent", e);
                    }
                }
            }
        });

        Button btnImportLocalMarkers = (Button)v.findViewById(R.id.btn_import_local_markers);
        btnImportLocalMarkers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mGoogleApiClient.isConnected()) {
                    IntentSender intentSender = Drive.DriveApi
                            .newOpenFileActivityBuilder()
                            .setMimeType(new String[]{"application/vnd.google-earth.kml+xml", "application/vnd.google-earth.kmz"})
                            .build(mGoogleApiClient);
                    try {
                        getActivity().startIntentSenderForResult(intentSender, REQUEST_CODE_OPEN_LOCAL, null, 0, 0, 0);
                    } catch (IntentSender.SendIntentException e) {
                        Log.w(TAG, "Unable to send intent", e);
                    }
                }
            }
        });

        /*
        mRemoteListView = (ListView) v.findViewById(R.id.import_remote_list);
        mLocalListView = (ListView) v.findViewById(R.id.import_local_list);

        List<String> remote_file_list = new ArrayList<String>();
        List<String> local_file_list = new ArrayList<String>();

        // copy assets to internal storage files
        copyAssets("kml");

        // add all files from internal storage
        File dir = new File(getActivity().getFilesDir(), "/"); // "/" -> could be subdir instead of root

        File files[] = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".kml");
            }
        });

        for (File f : files) {
            remote_file_list.add(f.getName());
            local_file_list.add(f.getName());
        }

        mRemoteListAdapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item, remote_file_list);
        mLocalListAdapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item, local_file_list);
        mRemoteListView.setAdapter(mRemoteListAdapter);
        mLocalListView.setAdapter(mLocalListAdapter);

        mRemoteListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                TextView v = (TextView)view.findViewById(R.id.list_item);
                String filename = v.getText().toString();
                //String path = "kml" + File.separatorChar + filename;

                SharedPreferences sharedPref = getActivity().getPreferences(DislocatorApplication.getAppContext().MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(getResources().getString(R.string.load_remote), filename);
                editor.commit();
                openMap();
            }
        });

        mLocalListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                TextView v = (TextView)view.findViewById(R.id.list_item);
                String filename = v.getText().toString();
                //String path = "kml" + File.separatorChar + filename;

                SharedPreferences sharedPref = getActivity().getPreferences(DislocatorApplication.getAppContext().MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(getResources().getString(R.string.load_local), filename);
                editor.commit();
                openMap();
            }
        });

        */

        return v;
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        DislocatorActivity activity = (DislocatorActivity)getActivity();

        switch (requestCode) {
            case REQUEST_CODE_OPEN_LOCAL:
                if (resultCode == RESULT_OK) {

                    activity.driveIdLocal = ((DriveId)(data.getParcelableExtra(
                            OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID))).encodeToString();

                    openMap();
                }
                break;
            case REQUEST_CODE_OPEN_REMOTE:
                if (resultCode == RESULT_OK) {

                    activity.driveIdRemote =
                            ((DriveId)(data.getParcelableExtra(
                            OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID))).encodeToString();

                    openMap();
                }

                break;
            default:
                break;
        }


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
                File outFile = new File(getActivity().getFilesDir(), filename);
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
