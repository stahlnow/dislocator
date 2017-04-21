package com.stahlnow.android.dislocator;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;


public class ExportFragment extends Fragment implements FragmentCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = ExportFragment.class.getSimpleName();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_export,container,false);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        final String dateString = sdf.format(new Date());

        // checkPermissions(); // ask for storage permission

        Button btnExportLocalMarkers = (Button)v.findViewById(R.id.btn_export_local_markers);
        btnExportLocalMarkers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if (checkPermissions()) {
                    final File file = new File(getContext().getFilesDir(), getResources().getString(R.string.temporary_local_file));
                    String filename = "dislocator_local_" + dateString + ".kml";
                    exportFile(file, filename);
                //}
            }
        });

        Button btnExportRemoteMarkers = (Button)v.findViewById(R.id.btn_export_remote_markers);
        btnExportRemoteMarkers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if (checkPermissions()) {
                    final File file = new File(getContext().getFilesDir(), getResources().getString(R.string.temporary_remote_file));
                    String filename = "dislocator_remote_" + dateString + ".kml";
                    exportFile(file, filename);
                //}
            }
        });

        Button btnExportCombinedMarkers = (Button)v.findViewById(R.id.btn_export_combined_markers);
        btnExportCombinedMarkers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if (checkPermissions()) {
                    final File file = new File(getContext().getFilesDir(), getResources().getString(R.string.temporary_combined_file));
                    String filename = "dislocator_combined_" + dateString + ".kml";
                    exportFile(file, filename);
                //}
            }
        });

        return v;

    }


    private void exportFile(final File tempFile, String filename) {

        if (!tempFile.exists())
        {
            exportOnError();
            return;
        }


        //String root = Environment.getExternalStorageDirectory().toString();
        //final File dislocator_dir = new File(root + "/Dislocator/kml_export");
        //if (dislocator_dir.mkdirs()) { Log.d(TAG, "created directory."); }

        LayoutInflater li = LayoutInflater.from(getActivity());
        View promptsView = li.inflate(R.layout.export_dialog, null);
        AlertDialog.Builder titleDialogBuilder = new AlertDialog.Builder(getActivity());
        titleDialogBuilder.setView(promptsView);
        final EditText title = (EditText) promptsView.findViewById(R.id.title);
        title.setText(filename);

        titleDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String filename = title.getText().toString();

                                /*
                                File file = new File(dislocator_dir, filename);
                                if (file.exists()) file.delete();
                                // copy the file to external storage
                                try {
                                    copyFile(tempFile, file);
                                } catch (IOException e) {
                                    Log.e(TAG, e.toString());
                                    exportOnError();
                                    return;
                                }
                                */

                                Uri fileUri = null;
                                File outFile = new File(getContext().getFilesDir(), filename);
                                try {
                                    copyFile(tempFile, outFile);
                                } catch (IOException e) {
                                    Log.e(TAG, e.toString());
                                    exportOnError();
                                    return;
                                }


                                try {
                                    fileUri = FileProvider.getUriForFile(
                                            getActivity(),
                                            "com.stahlnow.android.dislocator.fileprovider",
                                            outFile);
                                } catch (IllegalArgumentException e) {
                                    Log.e(TAG, "The file can't be shared: " +
                                            outFile.getName());
                                }

                                if (fileUri != null) {
                                    Intent shareIntent = new Intent();
                                    shareIntent.setAction(Intent.ACTION_SEND);
                                    shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                                    shareIntent.setType("application/vnd.google-earth.kml+xml");
                                    startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.send_to)));
                                }

                                // exportPostfix(fileUri);


                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        });
        final AlertDialog alertDialog = titleDialogBuilder.create();
        alertDialog.show();
    }

    private void exportOnError() {
        Toast.makeText(DislocatorApplication.getAppContext(), getResources().getString(R.string.file_exported_error_no_data), Toast.LENGTH_LONG).show();
    }

    private void exportPostfix(Uri fileUri) {
        // let media folder know about the new file (optional)
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(fileUri);
        DislocatorApplication.getAppContext().sendBroadcast(intent);

        // show toast
        //Toast.makeText(DislocatorApplication.getAppContext(), getResources().getString(R.string.file_exported_successfully), Toast.LENGTH_LONG).show();
    }

    private boolean checkPermissions() {
        if (!isStoragePermissionGranted()) {
            Toast.makeText(DislocatorApplication.getAppContext(), getResources().getString(R.string.cannot_access_external_storage), Toast.LENGTH_LONG).show();
            return false;
        }

        if (!isExternalStorageWritable()) {
            Toast.makeText(DislocatorApplication.getAppContext(), getResources().getString(R.string.cannot_access_external_storage), Toast.LENGTH_LONG).show();
            return false;
        }

        return true;

    }


    private boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted");
                return true;
            } else {
                Log.v(TAG,"Permission is revoked");
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted");
            return true;
        }
    }


    /* Checks if external storage is available for read and write */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
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
