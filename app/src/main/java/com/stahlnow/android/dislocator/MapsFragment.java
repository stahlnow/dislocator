package com.stahlnow.android.dislocator;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.text.Html;
import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;
import com.stahlnow.android.dislocator.model.Bone;

import org.json.JSONException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;


public class MapsFragment extends Fragment
        implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        GoogleMap.OnMyLocationButtonClickListener,
        ClusterManager.OnClusterInfoWindowClickListener<Bone>,
        ClusterManager.OnClusterItemClickListener<Bone>,
        ClusterManager.OnClusterItemInfoWindowClickListener<Bone>,
        PermissionResultListener {

    private static final String TAG = MapsFragment.class.getSimpleName();

    private SharedPreferences mSharedPref;


    public GoogleMap mRemoteMap;
    public GoogleMap mLocalMap;

    private Marker mRemoteReferenceMarker;
    private Marker mLocalReferenceMarker;

    private ClusterManager<Bone> mLocalClusterManager;
    private ClusterManager<Bone> mRemoteClusterManager;


    // The entry point to Google Play services, used by the Places API and Fused Location Provider.
    private GoogleApiClient mGoogleApiClient;


    private LocationRequest mLocationRequest = null;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;
    // The time the last-known location was cached
    private String mLastUpdateTime;

    // Keys for storing activity state.
    private static final String KEY_LOCAL_CAMERA_POSITION = "local_camera_position";
    private static final String KEY_REMOTE_CAMERA_POSITION = "remote_camera_position";
    private static final String KEY_REQUESTING_LOCATION_UPDATES = "requesting_location_updates";
    private static final String KEY_LAST_UPDATE_TIME = "last_update_time";
    private static final String KEY_LAST_KNOWN_LOCATION = "last_known_location";

    private GoogleMap.OnMapLongClickListener mRemoteMapClickListener;


    private class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        // These are both viewgroups containing an ImageView with id "badge" and two TextViews with id
        // "title" and "snippet".
        private final View mWindow;

        private final View mContents;

        CustomInfoWindowAdapter() {
            mWindow = getActivity().getLayoutInflater().inflate(R.layout.custom_info_window, null);
            mContents = getActivity().getLayoutInflater().inflate(R.layout.custom_info_contents, null);
        }

        @Override
        public View getInfoWindow(Marker marker) {
            render(marker, mWindow);
            return mWindow;
        }


        @Override
        public View getInfoContents(Marker marker) {
            if (marker != null)
                render(marker, mContents);
            return mContents;
        }

        private void render(Marker marker, View view) {
            ((ImageView) view.findViewById(R.id.badge)).setImageResource(R.drawable.marker_image_32);

            String title = marker.getTitle();
            TextView titleUi = ((TextView) view.findViewById(R.id.title));
            if (title != null && !title.isEmpty()) {
                // Spannable string allows us to edit the formatting of the text.
                SpannableString titleText = new SpannableString(title);
                //titleText.setSpan(new ForegroundColorSpan(Color.RED), 0, titleText.length(), 0);
                titleUi.setText(titleText);
            } else {
                titleUi.setText("");
            }

            String snippet = marker.getSnippet();
            TextView snippetUi = ((TextView) view.findViewById(R.id.snippet));
            if (snippet != null && !snippet.isEmpty()) {

                //snippetText.setSpan(new ForegroundColorSpan(Color.MAGENTA), 0, 10, 0);
                //snippetText.setSpan(new ForegroundColorSpan(Color.BLUE), 12, snippet.length(), 0);

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    SpannableString snippetText = new SpannableString(Html.fromHtml(snippet, Html.FROM_HTML_MODE_LEGACY));
                    snippetUi.setText(snippetText);
                } else {
                    SpannableString snippetText = new SpannableString(Html.fromHtml(snippet));
                    snippetUi.setText(snippetText);
                }


            }
        }
    }

    private class BoneRenderer extends DefaultClusterRenderer<Bone> {
        private final IconGenerator mIconGenerator = new IconGenerator(getActivity().getApplicationContext());
        private final IconGenerator mClusterIconGenerator = new IconGenerator(getActivity().getApplicationContext());
        private final ImageView mImageView;
        private final ImageView mClusterImageView;
        private final int mDimension;

        public BoneRenderer(GoogleMap map, ClusterManager manager) {
            super(getActivity().getApplicationContext(), map, manager);

            View multiProfile = getActivity().getLayoutInflater().inflate(R.layout.multi_marker, null);
            mClusterIconGenerator.setContentView(multiProfile);
            mClusterImageView = (ImageView) multiProfile.findViewById(R.id.image);

            mImageView = new ImageView(getActivity().getApplicationContext());
            mDimension = (int) getResources().getDimension(R.dimen.custom_marker_image);
            mImageView.setLayoutParams(new ViewGroup.LayoutParams(mDimension, mDimension));
            int padding = (int) getResources().getDimension(R.dimen.custom_marker_padding);
            mImageView.setPadding(padding, padding, padding, padding);

            // TODO instead of nice mImageView we set marker image as background to have transparency marker
            //mIconGenerator.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.marker_image_32));
            mIconGenerator.setContentView(mImageView);

        }

        @Override
        protected void onBeforeClusterItemRendered(Bone bone, MarkerOptions markerOptions) {
            // Draw a single bone.
            mImageView.setImageResource(bone.getMarkerImage());
            Bitmap icon = mIconGenerator.makeIcon();
            markerOptions
                    .icon(BitmapDescriptorFactory.fromBitmap(icon))
                    .draggable(true)
                    .title(bone.getTitle());
        }

        @Override
        protected void onBeforeClusterRendered(Cluster<Bone> cluster, MarkerOptions markerOptions) {
            // Draw multiple people.
            // Note: this method runs on the UI thread. Don't spend too much time in here (like in this example).
            List<Drawable> markerImages = new ArrayList<Drawable>(Math.min(4, cluster.getSize()));
            int width = mDimension;
            int height = mDimension;

            for (Bone p : cluster.getItems()) {
                // Draw 4 at most.
                if (markerImages.size() == 4) break;
                Drawable drawable = getResources().getDrawable(p.getMarkerImage());
                drawable.setBounds(0, 0, width, height);
                markerImages.add(drawable);
            }
            MultiDrawable multiDrawable = new MultiDrawable(markerImages);
            multiDrawable.setBounds(0, 0, width, height);

            mClusterImageView.setImageDrawable(multiDrawable);
            Bitmap icon = mClusterIconGenerator.makeIcon(String.valueOf(cluster.getSize()));
            markerOptions
                    .alpha(0.9f)
                    .icon(BitmapDescriptorFactory.fromBitmap(icon));
        }

        @Override
        protected boolean shouldRenderAsCluster(Cluster cluster) {
            // Always render clusters.
            return cluster.getSize() > 1;
        }
    }

    private static class RemoteLocationSource implements LocationSource {
        public OnLocationChangedListener mListener;
        private boolean mPaused;

        @Override
        public void activate(OnLocationChangedListener listener) {
            mListener = listener;
        }

        @Override
        public void deactivate() {
            mListener = null;
        }

        public void onPause() {
            mPaused = true;
        }

        public void onResume() {
            mPaused = false;
        }
    }

    private RemoteLocationSource mRemoteLocationSource;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        View v = inflater.inflate(R.layout.fragment_maps, container, false);
        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());


        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {

            //mLocalCameraPosition = savedInstanceState.getParcelable(KEY_LOCAL_CAMERA_POSITION);

            // Update the value of mCurrentLocation from the Bundle and update the
            // UI to show the correct latitude and longitude.
            if (savedInstanceState.keySet().contains(KEY_LAST_KNOWN_LOCATION)) {
                // Since KEY_LAST_KNOWN_LOCATION was found in the Bundle, we can be sure that
                // mLastKnownLocation is not null.
                mLastKnownLocation = savedInstanceState.getParcelable(KEY_LAST_KNOWN_LOCATION);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(KEY_LAST_UPDATE_TIME)) {
                mLastUpdateTime = savedInstanceState.getString(
                        KEY_LAST_UPDATE_TIME);
            }
        }

    }


    @Override
    public void onActivityCreated(Bundle savedInstanceBundle) {
        super.onActivityCreated(savedInstanceBundle);

        // Build the Play services client for use by the Fused Location Provider and the Places API.
        // Use the addApi() method to request the Google Places API and the Fused Location Provider.
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
            mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                    .enableAutoManage(
                            getActivity(), // FragmentActivity
                            this) // OnConnectionFailedListener
                    .addConnectionCallbacks(this)
                    .addApi(LocationServices.API)
                    //.addApi(Places.GEO_DATA_API)
                    //.addApi(Places.PLACE_DETECTION_API)
                    .build();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.menu_add_marker:
                if (mLocalClusterManager != null) {
                    addBone(mLocalClusterManager);
                }
                return true;
            case R.id.menu_set_reference_point:

                Toast.makeText(getActivity(), getResources().getString(R.string.set_reference_point), Toast.LENGTH_LONG).show();

                mRemoteMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {

                    @Override
                    public void onMapLongClick(LatLng latLng) {

                        // reset to default listener
                        mRemoteMap.setOnMapLongClickListener(mRemoteMapClickListener);

                        if (mLastKnownLocation != null) {

                            // update remote map
                            if (mRemoteReferenceMarker != null) {
                                mRemoteReferenceMarker.remove();
                            }
                            mRemoteReferenceMarker = mRemoteMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title("Remote Reference Point")
                                    .icon(BitmapDescriptorFactory.defaultMarker(4))
                                    .draggable(false)
                            );
                            Location location = new Location("Remote Reference");
                            location.setLatitude(latLng.latitude);
                            location.setLongitude(latLng.longitude);

                            mRemoteMap.setLocationSource(mRemoteLocationSource);
                            mRemoteLocationSource.mListener.onLocationChanged(location);

                            mRemoteMap.animateCamera(CameraUpdateFactory.
                                    newLatLngZoom(latLng, mRemoteMap.getCameraPosition().zoom));

                            // update local map
                            if (mLocalReferenceMarker != null) {
                                mLocalReferenceMarker.remove();
                            }
                            LatLng local = new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
                            mLocalReferenceMarker = mLocalMap.addMarker(new MarkerOptions()
                                    .position(local)
                                    .title("Local Reference Point")
                                    .icon(BitmapDescriptorFactory.defaultMarker(4))
                                    .draggable(false)
                            );
                            mLocalMap.animateCamera(CameraUpdateFactory.
                                    newLatLngZoom(local, mLocalMap.getCameraPosition().zoom));
                        }
                        else {
                            Toast.makeText(DislocatorApplication.getAppContext(),
                                    "Can't get a location fix", Toast.LENGTH_SHORT).show();
                        }

                    }

                });

                return true;

            case R.id.menu_remove_local_markers:
                if (mLocalClusterManager != null) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(getResources().getString(R.string.menu_remove_local_markers))
                            .setMessage(getResources().getString(R.string.menu_confirm))
                            .setNegativeButton(android.R.string.cancel, null) // dismisses by default
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mLocalClusterManager.clearItems();
                                    mLocalClusterManager.cluster();
                                }
                            })
                            .create()
                            .show();
                }
                return true;

            case R.id.menu_remove_remote_markers:
                if (mRemoteClusterManager != null) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(getResources().getString(R.string.menu_remove_local_markers))
                            .setMessage(getResources().getString(R.string.menu_confirm))
                            .setNegativeButton(android.R.string.cancel, null) // dismisses by default
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mRemoteClusterManager.clearItems();
                                    mRemoteClusterManager.cluster();
                                }
                            })
                            .create()
                            .show();
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onStart() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((mGoogleApiClient.isConnected() || mGoogleApiClient.isConnecting())) {
            mGoogleApiClient.connect();
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {

            stopLocationUpdates();

            mGoogleApiClient.stopAutoManage(getActivity());
            mGoogleApiClient.disconnect();
        }

        // save markers files
        File local_file = new File(DislocatorApplication.getAppContext().getFilesDir(), "local.kml");
        File remote_file = new File(DislocatorApplication.getAppContext().getFilesDir(), "remote.kml");
        File combined_file = new File(DislocatorApplication.getAppContext().getFilesDir(), "combined.kml");

        Collection<Bone> all_items = null;

        try {
            Collection<Bone> items = mLocalClusterManager.getAlgorithm().getItems();
            all_items = mLocalClusterManager.getAlgorithm().getItems();
            MyBoneWriter.saveKML(local_file, items, "Dislocator");
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }

        try {
            Collection<Bone> items = mRemoteClusterManager.getAlgorithm().getItems();
            if (all_items != null)
                all_items.addAll(items);
            else {
                all_items = mRemoteClusterManager.getAlgorithm().getItems();
            }
            MyBoneWriter.saveKML(remote_file, items, "Dislocator");
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }

        try {
            MyBoneWriter.saveKML(combined_file, all_items, "Dislocator");
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }

        // save reference marker
        SharedPreferences sharedPref = getActivity().getPreferences(DislocatorApplication.getAppContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        if (mLocalReferenceMarker != null) {
            putDouble(editor, getResources().getString(R.string.local_ref_lat), mLocalReferenceMarker.getPosition().latitude);
            putDouble(editor, getResources().getString(R.string.local_ref_lng), mLocalReferenceMarker.getPosition().longitude);
        }
        if (mRemoteReferenceMarker != null) {
            putDouble(editor, getResources().getString(R.string.remote_ref_lat), mRemoteReferenceMarker.getPosition().latitude);
            putDouble(editor, getResources().getString(R.string.remote_ref_lng), mRemoteReferenceMarker.getPosition().longitude);
        }
        editor.commit();
    }


    @Override
    public void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.stopAutoManage(getActivity());
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.stopAutoManage(getActivity());
        mGoogleApiClient.disconnect();
    }


    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mLocalMap != null) {
            outState.putParcelable(KEY_LOCAL_CAMERA_POSITION, mLocalMap.getCameraPosition());
        }
        outState.putParcelable(KEY_LAST_KNOWN_LOCATION, mLastKnownLocation);
        outState.putString(KEY_LAST_UPDATE_TIME, mLastUpdateTime);

        super.onSaveInstanceState(outState);
    }

    /**
     * Builds the map when the Google Play services client is successfully connected.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        // Build the maps.
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        RemoteMapFragment mapViewRemoteFragment = (RemoteMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.fragment_map_remote);
        mapViewRemoteFragment.getMapAsync(mapViewRemoteFragment);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        LocalMapFragment mapViewLocalFragment = (LocalMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.fragment_map_local);
        mapViewLocalFragment.getMapAsync(mapViewLocalFragment);


        if (mLocationRequest == null) { // if not created yet, create request
            createLocationRequest();
            startLocationUpdates();
            mRemoteLocationSource.onResume();
        }

    }

    /**
     * Handles failure to connect to the Google Play services client.
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        // Refer to the reference doc for ConnectionResult to see what error codes might
        // be returned in onConnectionFailed.
        Log.d(TAG, "Play services connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    /**
     * Handles suspension of the connection to the Google Play services client.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        stopLocationUpdates();
        Log.d(TAG, "Play services connection suspended");
    }


    public void localMapReady(GoogleMap map) {
        mLocalMap = map;
        mLocalMap.getUiSettings().setMapToolbarEnabled(false);

        enableMyLocationLocalMap();

        mLocalClusterManager = new ClusterManager<Bone>(DislocatorApplication.getAppContext(), mLocalMap);

        mLocalClusterManager.setRenderer(new BoneRenderer(mLocalMap, mLocalClusterManager));
        mLocalMap.setOnCameraIdleListener(mLocalClusterManager);


        mLocalMap.setOnMarkerClickListener(mLocalClusterManager);
        mLocalMap.setOnInfoWindowClickListener(mLocalClusterManager);
        mLocalClusterManager.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<Bone>() {
            @Override
            public boolean onClusterClick(Cluster<Bone> cluster) {
                return onMyClusterClick(mLocalMap, cluster);
            }
        });
        mLocalClusterManager.setOnClusterInfoWindowClickListener(this);
        mLocalClusterManager.setOnClusterItemClickListener(this);
        mLocalClusterManager.setOnClusterItemInfoWindowClickListener(this);

        //mLocalMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());


        mLocalMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng point) {

                addBone(mLocalClusterManager, point);

                /*
                if (mSharedPref.getBoolean(getString(R.string.pref_sync_markers), false)) {

                    if (mLocalReferenceMarker != null) {

                        double latDiff = point.latitude - mLocalReferenceMarker.getPosition().latitude;
                        double lngDiff = point.longitude - mLocalReferenceMarker.getPosition().longitude;
                        Marker rm = mRemoteMap.addMarker(new MarkerOptions()
                                .position(new LatLng(
                                        mRemoteReferenceMarker.getPosition().latitude + latDiff,
                                        mRemoteReferenceMarker.getPosition().longitude + lngDiff))
                                .title("a remote Marker")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                                .draggable(true)
                        );
                    }
                }
                */

            }
        });


        GoogleMap.OnCameraMoveListener listener = new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                try {
                    Boolean syncZoomLevel = mSharedPref.getBoolean(getString(R.string.pref_sync_zoom_level), false);
                    if (syncZoomLevel) {
                        if (mRemoteMap != null) {
                            mRemoteMap.animateCamera(CameraUpdateFactory.zoomTo(mLocalMap.getCameraPosition().zoom));
                        }
                    }
                } catch (java.lang.IllegalStateException e) {
                    Log.e(TAG, e.toString());
                }

            }
        };

        mLocalMap.setOnCameraMoveListener(listener);


        // check if we should load something
        SharedPreferences sharedPref = getActivity().getPreferences(DislocatorApplication.getAppContext().MODE_PRIVATE);
        String path = sharedPref.getString(getResources().getString(R.string.load_local), "");
        if (path != "") {
            // clear value
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(getResources().getString(R.string.load_local), "");
            editor.commit();

            try {
                AssetManager assetManager = DislocatorApplication.getAppContext().getAssets();
                readKMLItems(assetManager.open(path), mLocalMap, mLocalClusterManager);
            } catch (FileNotFoundException e) {
                Log.w(TAG, e.toString());
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            } catch (XmlPullParserException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            // read markers from auto-save
            File file = new File(DislocatorApplication.getAppContext().getFilesDir(), "local.kml");
            try {
                FileInputStream fi = new FileInputStream(file);
                try {
                    readKMLItems(fi, mLocalMap, mLocalClusterManager);
                } catch (IOException e) {
                    Log.e(TAG, "Can't read file: " + file.getPath());
                } catch (XmlPullParserException e) {
                    Log.e(TAG, "KML file could not be parsed: " + e.toString());
                }

            } catch (FileNotFoundException e) {
                Log.w(TAG, e.toString());
            }
        }


        // restore reference marker
        double refLat = getDouble(sharedPref, getResources().getString(R.string.local_ref_lat), -1000.0);
        double refLng = getDouble(sharedPref, getResources().getString(R.string.local_ref_lng), -1000.0);
        if (refLat != -1000.0 || refLng != -1000.0) {
            LatLng pos = new LatLng(refLat, refLng);
            mLocalReferenceMarker = mLocalMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title("Local Reference Point")
                    .icon(BitmapDescriptorFactory.defaultMarker(4))
                    .draggable(false)
            );
        }


        animateCameraToCluster(mLocalClusterManager, mLocalMap);

    }

    public void remoteMapReady(GoogleMap map) {

        mRemoteMap = map;
        mRemoteMap.getUiSettings().setMapToolbarEnabled(false);
        mRemoteMap.getUiSettings().setZoomControlsEnabled(!mSharedPref.getBoolean(getString(R.string.pref_sync_zoom_level), false));

        enableMyLocationRemoteMap();

        mRemoteClusterManager = new ClusterManager<Bone>(DislocatorApplication.getAppContext(), mRemoteMap);

        mRemoteClusterManager.setRenderer(new BoneRenderer(mRemoteMap, mRemoteClusterManager));
        mRemoteMap.setOnCameraIdleListener(mRemoteClusterManager);


        mRemoteMap.setOnMarkerClickListener(mRemoteClusterManager);
        mRemoteMap.setOnInfoWindowClickListener(mRemoteClusterManager);
        mRemoteClusterManager.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<Bone>() {
            @Override
            public boolean onClusterClick(Cluster<Bone> cluster) {
                return onMyClusterClick(mRemoteMap, cluster);
            }
        });
        mRemoteClusterManager.setOnClusterInfoWindowClickListener(this);
        mRemoteClusterManager.setOnClusterItemClickListener(this);
        mRemoteClusterManager.setOnClusterItemInfoWindowClickListener(this);


        //mRemoteMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());


        // re-cluster after adding items
        mRemoteClusterManager.cluster();


        mRemoteMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(51.51699182763696, -0.06479247100651264),
                mRemoteMap.getCameraPosition().zoom
        ));

        mRemoteMapClickListener = new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                addBone(mRemoteClusterManager, latLng);
            }
        };

        mRemoteMap.setOnMapLongClickListener(mRemoteMapClickListener);


        // check if we should load something
        SharedPreferences sharedPref = getActivity().getPreferences(DislocatorApplication.getAppContext().MODE_PRIVATE);
        String path = sharedPref.getString(getResources().getString(R.string.load_remote), "");
        if (path != "") {
            // clear value
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(getResources().getString(R.string.load_remote), "");
            editor.commit();

            try {
                AssetManager assetManager = DislocatorApplication.getAppContext().getAssets();
                readKMLItems(assetManager.open(path), mRemoteMap, mRemoteClusterManager);
            } catch (FileNotFoundException e) {
                Log.w(TAG, e.toString());
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            } catch (XmlPullParserException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            // read markers from auto-save
            File file = new File(DislocatorApplication.getAppContext().getFilesDir(), "remote.kml");
            try {
                FileInputStream fi = new FileInputStream(file);
                try {
                    readKMLItems(fi, mRemoteMap, mRemoteClusterManager);
                } catch (IOException e) {
                    Log.e(TAG, "Can't read file: " + file.getPath());
                } catch (XmlPullParserException e) {
                    Log.e(TAG, "KML file could not be parsed: " + e.toString());
                }

            } catch (FileNotFoundException e) {
                Log.w(TAG, e.toString());
            }
        }


        // restore reference marker
        double refLat = getDouble(sharedPref, getResources().getString(R.string.remote_ref_lat), -1000.0);
        double refLng = getDouble(sharedPref, getResources().getString(R.string.remote_ref_lng), -1000.0);
        if (refLat != -1000.0 || refLng != -1000.0) {
            LatLng pos = new LatLng(refLat, refLng);
            mRemoteReferenceMarker = mRemoteMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title("Remote Reference Point")
                    .icon(BitmapDescriptorFactory.defaultMarker(4))
                    .draggable(false)
            );

            Location location = new Location("Remote Reference");
            location.setLatitude(pos.latitude);
            location.setLongitude(pos.longitude);

            mRemoteMap.setLocationSource(mRemoteLocationSource);
            mRemoteLocationSource.mListener.onLocationChanged(location);
        }


        animateCameraToCluster(mRemoteClusterManager, mRemoteMap);
    }


    private void animateCameraToCluster(ClusterManager<Bone> manager, GoogleMap map) {

        if (manager.getAlgorithm().getItems().size() == 0)
            return;

        // zoom to bounds
        LatLngBounds.Builder builder = LatLngBounds.builder();
        for (Bone bone : manager.getAlgorithm().getItems()) {
            builder.include(bone.getPosition());
        }
        // Get the LatLngBounds
        final LatLngBounds bounds = builder.build();

        // Animate camera to the bounds
        try {
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }


    private boolean onMyClusterClick(GoogleMap map, Cluster<Bone> cluster) {
        // Zoom in the cluster. Need to create LatLngBounds and including all the cluster items
        // inside of bounds, then animate to center of the bounds.

        // Create the builder to collect all essential cluster items for the bounds.
        LatLngBounds.Builder builder = LatLngBounds.builder();
        for (ClusterItem item : cluster.getItems()) {
            builder.include(item.getPosition());
        }
        // Get the LatLngBounds
        final LatLngBounds bounds = builder.build();

        // Animate camera to the bounds
        try {
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public void onClusterInfoWindowClick(Cluster<Bone> cluster) {
        //Log.d(TAG, "onClusterInfoWindowClick");
    }

    @Override
    public boolean onClusterItemClick(Bone item) {
        //Log.d(TAG, "onClusterItemClick");
        return false;
    }

    private void reloadMarkers(GoogleMap map, ClusterManager manager) {
        Collection<Bone> items = manager.getAlgorithm().getItems();
        map.clear();
        manager.clearItems();
        manager.addItems(items);
        manager.cluster();
    }

    @Override
    public void onClusterItemInfoWindowClick(final Bone item) {
        // edit marker
        LayoutInflater li = LayoutInflater.from(getActivity());
        View promptsView = li.inflate(R.layout.add_bone_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(promptsView);
        final EditText boneName = (EditText) promptsView.findViewById(R.id.boneName);
        final EditText boneDescription = (EditText) promptsView.findViewById(R.id.boneDescription);
        boneName.setText(item.getName());
        boneDescription.setText(item.getSnippet());


        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                item.setName(boneName.getText().toString());
                                item.setDescription(boneDescription.getText().toString());
                                //reloadMarkers(mLocalMap, mLocalClusterManager);
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        final AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

        final Button btnDeleteMarker = (Button) promptsView.findViewById(R.id.btn_delete_marker);
        btnDeleteMarker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mLocalClusterManager.removeItem(item);
                    mLocalClusterManager.cluster();
                    alertDialog.dismiss();
                } catch (Exception e) {

                }

                try {
                    mRemoteClusterManager.removeItem(item);
                    mRemoteClusterManager.cluster();
                    alertDialog.dismiss();
                } catch (Exception e) {

                }

            }
        });

    }


    public void addBone(final ClusterManager manager) {
        LatLng point = new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
        addBone(manager, point);
    }

    public void addBone(final ClusterManager manager, final LatLng point) {

        LayoutInflater li = LayoutInflater.from(getActivity());
        View promptsView = li.inflate(R.layout.add_bone_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(promptsView);
        final EditText boneName = (EditText) promptsView.findViewById(R.id.boneName);
        final EditText boneDescription = (EditText) promptsView.findViewById(R.id.boneDescription);

        final Button btnDeleteMarker = (Button) promptsView.findViewById(R.id.btn_delete_marker);
        btnDeleteMarker.setVisibility(View.GONE);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                Bone bone = new Bone(boneName.getText().toString(), "", boneDescription.getText().toString(), point);
                                manager.addItem(bone);
                                manager.cluster();

                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }


    private void readKMLItems(InputStream inputStream, GoogleMap map, ClusterManager manager) throws IOException, XmlPullParserException {

        if (manager == null)
            return;

        manager.clearItems();

        List<Bone> items = new MyBoneReader().readKML(inputStream, map);

        for (Bone bone : items) {
            if (bone.getPosition() != null) {
                manager.addItem(bone);
            }
        }

        manager.cluster();

    }

    private void readJSONItems(String path, ClusterManager manager) throws IOException, JSONException {
        AssetManager assetManager = DislocatorApplication.getAppContext().getAssets();
        InputStream inputStream = assetManager.open(path);
        List<Bone> items = new MyBoneReader().readJSON(inputStream);
        manager.addItems(items);
    }


    /*******************************************************************************************
     *
     * Location update handling
     *
     *******************************************************************************************/
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @SuppressWarnings({"MissingPermission"})
    protected void startLocationUpdates() {
        if (requestLocationPermission(1)) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }
        mRemoteLocationSource = new MapsFragment.RemoteLocationSource();
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);

        if (mRemoteLocationSource != null)
            mRemoteLocationSource.onPause();
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(DislocatorApplication.getAppContext(), "Get location fix", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    @Override
    public void onLocationChanged(Location location) {

        mLastKnownLocation = location; // this maybe null in rare cases when a location is not available
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

        if (mLastKnownLocation != null) {
            Log.v(TAG, "onLocationChanged: " + location.toString());
            // update local map
            if (mLocalMap != null && mSharedPref.getBoolean(getString(R.string.pref_center_local_map), true)) {
                mLocalMap.animateCamera(CameraUpdateFactory.
                        newLatLngZoom(
                                new LatLng(location.getLatitude(), location.getLongitude()),
                                mLocalMap.getCameraPosition().zoom));
            }

            // update remote map
            if (mRemoteMap != null && mRemoteLocationSource != null && mLocalReferenceMarker != null && mRemoteReferenceMarker != null) {
                Log.d(TAG, "onLocationCHanged");
                double latDiff = location.getLatitude() - mLocalReferenceMarker.getPosition().latitude;
                double lngDiff = location.getLongitude() - mLocalReferenceMarker.getPosition().longitude;
                Location remoteLocation = new Location("Remote Fix");
                remoteLocation.setLatitude(mRemoteReferenceMarker.getPosition().latitude + latDiff);
                remoteLocation.setLongitude(mRemoteReferenceMarker.getPosition().longitude + lngDiff);
                mRemoteLocationSource.mListener.onLocationChanged(remoteLocation);

                if (mRemoteMap != null && mSharedPref.getBoolean(getString(R.string.pref_center_remote_map), true)) {
                    mRemoteMap.animateCamera(CameraUpdateFactory.
                            newLatLngZoom(
                                    new LatLng(remoteLocation.getLatitude(), remoteLocation.getLongitude()),
                                    mRemoteMap.getCameraPosition().zoom));
                }
            }
        }

    }


    @SuppressWarnings({"MissingPermission"})
    private void enableMyLocationLocalMap() {
        if (requestLocationPermission(1) && mLocalMap != null) {
            mLocalMap.setMyLocationEnabled(true);
            mLocalMap.setOnMyLocationButtonClickListener(this);
        }
    }

    @SuppressWarnings({"MissingPermission"})
    private void enableMyLocationRemoteMap() {
        if (requestLocationPermission(2) && mRemoteMap != null) {
            mRemoteMap.setMyLocationEnabled(true);
            mRemoteMap.setOnMyLocationButtonClickListener(this);
        }
    }

    private boolean requestLocationPermission(int requestCode) {

        Activity activity = getActivity();
        if (activity != null && activity instanceof DislocatorActivity) {

            DislocatorActivity mainActivity = (DislocatorActivity) activity;
            mainActivity.setPermissionResultListener(this);

            // returns true if we have access
            return mainActivity.requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, requestCode);

        }
        return false;

    }

    @Override
    public void onPermissionResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {
            case 1: { // local map
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableMyLocationLocalMap();
                }
            }
            case 2: { // remote map
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableMyLocationRemoteMap();
                }
            }
        }

    }

    // SharedPreferences Helper
    SharedPreferences.Editor putDouble(final SharedPreferences.Editor edit, final String key, final double value) {
        return edit.putLong(key, Double.doubleToRawLongBits(value));
    }

    double getDouble(final SharedPreferences prefs, final String key, final double defaultValue) {
        return Double.longBitsToDouble(prefs.getLong(key, Double.doubleToLongBits(defaultValue)));
    }
}
