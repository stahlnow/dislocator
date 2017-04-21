package com.stahlnow.android.dislocator;

import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ImportFragment extends Fragment {

    private static final String TAG = ImportFragment.class.getSimpleName();

    private ListView mRemoteListView;
    private ListView mLocalListView;
    private ArrayAdapter<String> mRemoteListAdapter;
    private ArrayAdapter<String> mLocalListAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_import,container,false);

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

        return v;
    }


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

    private void openMap() {
        MapsFragment mapFragment = new MapsFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.content, mapFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }
}
