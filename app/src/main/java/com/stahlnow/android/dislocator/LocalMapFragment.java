package com.stahlnow.android.dislocator;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

public class LocalMapFragment extends SupportMapFragment implements OnMapReadyCallback {

    private static final String TAG = LocalMapFragment.class.getSimpleName();

    @Override
        public void onMapReady(GoogleMap googleMap) {

        Log.d(TAG, "onMapReady");

        MapsFragment mf = (MapsFragment)getParentFragment();

        mf.localMapReady(googleMap);

    }
}