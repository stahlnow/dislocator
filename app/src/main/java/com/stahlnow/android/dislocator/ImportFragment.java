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
import java.io.IOException;
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


        final AssetManager am = DislocatorApplication.getAppContext().getAssets();

        List<String> remote_file_list = new ArrayList<String>();
        List<String> local_file_list = new ArrayList<String>();

        try {
            remote_file_list.addAll( Arrays.asList(am.list("kml")) );
            local_file_list.addAll( Arrays.asList(am.list("kml")) );
        } catch (IOException e) {
            Log.w(TAG, e.toString());
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
                String path = "kml" + File.separatorChar + filename;

                SharedPreferences sharedPref = getActivity().getPreferences(DislocatorApplication.getAppContext().MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(getResources().getString(R.string.load_remote), path);
                editor.commit();
                openMap();
            }
        });

        mLocalListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                TextView v = (TextView)view.findViewById(R.id.list_item);
                String filename = v.getText().toString();
                String path = "kml" + File.separatorChar + filename;

                SharedPreferences sharedPref = getActivity().getPreferences(DislocatorApplication.getAppContext().MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(getResources().getString(R.string.load_local), path);
                editor.commit();
                openMap();
            }
        });

        return v;
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
