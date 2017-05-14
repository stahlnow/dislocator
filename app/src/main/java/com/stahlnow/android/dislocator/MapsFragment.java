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
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.os.EnvironmentCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.CardView;
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
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.location.places.ui.SupportPlaceAutocompleteFragment;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanoramaFragment;
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.StreetViewPanoramaCamera;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;
import com.stahlnow.android.dislocator.DislocatorActivity.IMPORT_TO;
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
        PermissionResultListener, OnStreetViewPanoramaReadyCallback

{

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
    private LatLng mLatLngDefaultRemoteReference;
    private boolean mLocationAccessGranted = false;
    private CameraPosition mLocalCameraPosition = null;
    private CameraPosition mRemoteCameraPosition = null;
    private boolean mStreetViewActiveLocal = false;
    private boolean mStreetViewActiveRemote = false;
    private SupportPlaceAutocompleteFragment mSearchFragment;
    private CardView mSearchWrapperView;
    private boolean mSearchOnLocalMap = false;

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
            //MultiDrawable multiDrawable = new MultiDrawable(markerImages);
            //multiDrawable.setBounds(0, 0, width, height);
            //mClusterImageView.setImageDrawable(multiDrawable);
            mClusterImageView.setImageDrawable(getResources().getDrawable(R.drawable.marker_image));
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
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        // 65.337512 -15.852263
        mLatLngDefaultRemoteReference = new LatLng(65.337512, -15.852263);

        // DEBUG
//        SharedPreferences sharedPref = getActivity().getPreferences(DislocatorApplication.getAppContext().MODE_PRIVATE);
//        SharedPreferences.Editor editor = sharedPref.edit();
//        editor.clear();
//        editor.commit();
        // DEBUG

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        View v = inflater.inflate(R.layout.fragment_maps, container, false);
        return v;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        Log.d(TAG, "onActivityCreated");

        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(KEY_LOCAL_CAMERA_POSITION)) {
                mLocalCameraPosition = savedInstanceState.getParcelable(KEY_LOCAL_CAMERA_POSITION);
            }
            if (savedInstanceState.keySet().contains(KEY_REMOTE_CAMERA_POSITION)) {
                mRemoteCameraPosition = savedInstanceState.getParcelable(KEY_REMOTE_CAMERA_POSITION);
            }
            if (savedInstanceState.keySet().contains(KEY_LAST_KNOWN_LOCATION)) {
                mLastKnownLocation = savedInstanceState.getParcelable(KEY_LAST_KNOWN_LOCATION);
            }
            if (savedInstanceState.keySet().contains(KEY_LAST_UPDATE_TIME)) {
                mLastUpdateTime = savedInstanceState.getString(
                        KEY_LAST_UPDATE_TIME);
            }
        } else {
            DislocatorActivity activity = (DislocatorActivity)getActivity();
            mLocalCameraPosition = activity.mSettings.getParcelable(getString(R.string.key_local_camera_position));
            mRemoteCameraPosition = activity.mSettings.getParcelable(getString(R.string.key_remote_camera_position));
        }

        // Build the Play services client for use by the Fused Location Provider and the Places API.
        // Use the addApi() method to request the Google Places API and the Fused Location Provider.
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
            mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                    .enableAutoManage(
                            getActivity(),  // FragmentActivity
                            0,              // client id
                            this)           // OnConnectionFailedListener
                    .addConnectionCallbacks(this)
                    .addApi(LocationServices.API)

                    // enable search
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)

                    // enable drive
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)


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
                            createLocalReferenceMarker(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()), true);
                            createRemoteReferenceMarker(latLng, true);
                            if (mRemoteLocationSource != null) {
                                mRemoteMap.setLocationSource(mRemoteLocationSource);
                                Location remoteLocation = new Location("Remote Fix");
                                remoteLocation.setLatitude(latLng.latitude);
                                remoteLocation.setLongitude(latLng.longitude);
                                mRemoteLocationSource.mListener.onLocationChanged(remoteLocation);
                            }
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

            /*
            case R.id.menu_search_local_map:
                if (mSearchWrapperView != null) {
                    mSearchWrapperView.setVisibility(View.VISIBLE);
                    mSearchOnLocalMap = true;
                }

                return true;
            */

            case R.id.menu_search_remote_map:
                if (mSearchWrapperView != null) {
                    mSearchWrapperView.setVisibility(View.VISIBLE);
                    mSearchOnLocalMap = false;
                }

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onPause() {

        Log.d(TAG, "onPause");

        super.onPause();

        DislocatorActivity activity = (DislocatorActivity)getActivity();
        if (mLocalMap != null && mRemoteMap != null && activity.mSettings != null) {
            activity.mSettings.putParcelable(getString(R.string.key_local_camera_position), mLocalMap.getCameraPosition());
            activity.mSettings.putParcelable(getString(R.string.key_remote_camera_position), mRemoteMap.getCameraPosition());
        }

        // stop google services: this will disconnect and stop location updates see @onConnectionSuspended
        if (requestLocationPermission(DislocatorApplication.REQUEST_LOCATION)) // hack to not disconnect on permission dialog
            mGoogleApiClient.stopAutoManage(getActivity()); //TODO

        // save markers files
        File local_file = new File(DislocatorApplication.getAppContext().getFilesDir(), getResources().getString(R.string.temporary_local_file));
        File remote_file = new File(DislocatorApplication.getAppContext().getFilesDir(), getResources().getString(R.string.temporary_remote_file));
        File combined_file = new File(DislocatorApplication.getAppContext().getFilesDir(), getResources().getString(R.string.temporary_combined_file));

        Collection<Bone> all_items = null;

        if (mLocalClusterManager != null) {
            try {
                Collection<Bone> items = mLocalClusterManager.getAlgorithm().getItems();
                all_items = mLocalClusterManager.getAlgorithm().getItems();
                MyBoneWriter.saveKML(local_file, items, "Dislocator");
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }

        if (mRemoteClusterManager != null) {
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
        }

        if (mLocalClusterManager != null && mRemoteClusterManager != null) {
            try {
                MyBoneWriter.saveKML(combined_file, all_items, "Dislocator");
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }

    }


    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");

        super.onSaveInstanceState(outState);

        if (mLocalMap != null) {
            outState.putParcelable(KEY_LOCAL_CAMERA_POSITION, mLocalMap.getCameraPosition());
        }

        if (mRemoteMap != null) {
            outState.putParcelable(KEY_REMOTE_CAMERA_POSITION, mRemoteMap.getCameraPosition());
        }

        outState.putParcelable(KEY_LAST_KNOWN_LOCATION, mLastKnownLocation);
        outState.putString(KEY_LAST_UPDATE_TIME, mLastUpdateTime);
    }

    /**
     * Builds the map when the Google Play services client is successfully connected.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected");
        // Build the maps.
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        RemoteMapFragment mapViewRemoteFragment = (RemoteMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.fragment_map_remote);
        mapViewRemoteFragment.getMapAsync(mapViewRemoteFragment);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        LocalMapFragment mapViewLocalFragment = (LocalMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.fragment_map_local);
        mapViewLocalFragment.getMapAsync(mapViewLocalFragment);


        // Setup search widget
        mSearchWrapperView = (CardView) getView().findViewById(R.id.search_wrapper_view);
        mSearchFragment = (SupportPlaceAutocompleteFragment) getChildFragmentManager()
                .findFragmentById(R.id.search_autocomplete_fragment);
        mSearchFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {

                String placeDetailsStr = place.getName() + "\n"
                        + place.getId() + "\n"
                        + place.getLatLng().toString() + "\n"
                        + place.getAddress() + "\n"
                        + place.getAttributions();
                Log.i(TAG, "Found: " + placeDetailsStr);

                if (mSearchOnLocalMap) {
                    mLocalMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            place.getLatLng(), 12
                    ));
                } else {
                    mRemoteMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            place.getLatLng(), 12
                    ));
                }

                mSearchWrapperView.setVisibility(View.GONE);

            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });

    }

    /**
     * Handles failure to connect to the Google Play services client.
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.w(TAG, "Play services connection failed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());
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
            }
        });


        GoogleMap.OnCameraMoveListener listener = new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {

                CameraPosition cameraPosition = mLocalMap.getCameraPosition();
                if (cameraPosition.zoom == mLocalMap.getMaxZoomLevel()) {
                    enterStreetView(R.id.fragment_map_local, true);
                } else if(cameraPosition.zoom > 18.0) {
                    mLocalMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                } else {
                    mLocalMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                }

                // sync zoom levels
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

        // read markers from auto-save
        File file = new File(DislocatorApplication.getAppContext().getFilesDir(), getResources().getString(R.string.temporary_local_file));

        DislocatorActivity activity = (DislocatorActivity)getActivity();
        if (activity.sdCardFile != null && (activity.import_to == IMPORT_TO.LOCAL || activity.import_to == IMPORT_TO.REMOTE_AND_LOCAL)) {
            file = new File(Environment.getExternalStorageDirectory(), "Dislocator/" + activity.sdCardFile);
            Log.d(TAG, file.toString());
        }
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


        // restore reference marker
        LatLng loc = getLocalReferenceLatLng();
        if (loc != null) {
            // restore from preferences
            createLocalReferenceMarker(loc, false);
        } else if (mLastKnownLocation != null) {
            createLocalReferenceMarker(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()), true);
        }


        // restore camera position
        if (mLocalCameraPosition != null) {
            mLocalMap.animateCamera(CameraUpdateFactory.newCameraPosition(mLocalCameraPosition));
        } else {
            // animate camera above switzerland
            mLocalMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(46.491237, 8.121843),
                    mLocalMap.getCameraPosition().zoom
            ));
        }

        onMapsReady();
    }


    public void remoteMapReady(GoogleMap map) {

        mRemoteMap = map;
        mRemoteMap.getUiSettings().setMapToolbarEnabled(false);
        mRemoteMap.getUiSettings().setZoomControlsEnabled(!mSharedPref.getBoolean(getString(R.string.pref_sync_zoom_level), false));


        // WORKS
        //OpenStreetMapTileProvider provider = new OpenStreetMapTileProvider();
        //TileOverlayOptions opts = new TileOverlayOptions();
        //opts.tileProvider(provider);
        //TileOverlay overlay = mRemoteMap.addTileOverlay(opts);

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


        mRemoteMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                CameraPosition cameraPosition = mRemoteMap.getCameraPosition();
                if (cameraPosition.zoom == mRemoteMap.getMaxZoomLevel()) {
                    enterStreetView(R.id.fragment_map_remote, false);
                } else if(cameraPosition.zoom > 18.0) {
                    mRemoteMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                } else {
                    mRemoteMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                }
            }
        });


        mRemoteMapClickListener = new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                addBone(mRemoteClusterManager, latLng);
            }
        };

        mRemoteMap.setOnMapLongClickListener(mRemoteMapClickListener);

        // check if we should load something
        // read markers from auto-save
        File file = new File(DislocatorApplication.getAppContext().getFilesDir(), getResources().getString(R.string.temporary_remote_file));
        DislocatorActivity activity = (DislocatorActivity)getActivity();
        if (activity.sdCardFile != null && (activity.import_to == IMPORT_TO.REMOTE || activity.import_to == IMPORT_TO.REMOTE_AND_LOCAL)) {
            file = new File(Environment.getExternalStorageDirectory(), "Dislocator/" + activity.sdCardFile);
            Log.d(TAG, file.toString());
        }
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

        // restore reference marker
        LatLng loc = getRemoteReferenceLatLng();
        if (loc != null) {
            // restore from preferences
            createRemoteReferenceMarker(loc, false);
        } else if (mLastKnownLocation != null) {
            // try to set default ref point
            createRemoteReferenceMarker(mLatLngDefaultRemoteReference, true);
        }


        // restore camera position
        if (mRemoteCameraPosition != null) {
            mRemoteMap.animateCamera(CameraUpdateFactory.newCameraPosition(mRemoteCameraPosition));
        } else {
            // animate camera above iceland 64°57'46.97" N  19°01'15.04" W
            // before animating it later to the reference point
            mRemoteMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(64.574697, -19.011504),
                    mRemoteMap.getCameraPosition().zoom
            ));
        }

        onMapsReady();
    }

    private void onMapsReady() {
        if (mLocalMap == null || mRemoteMap == null)
            return;

        // ask for location access
        if (requestLocationPermission(DislocatorApplication.REQUEST_LOCATION)) {
            createLocationRequest();
            startLocationUpdates();
            enableMyLocationLocalMap();
            enableMyLocationRemoteMap();
        }

        // check if we should load something
        DislocatorActivity activity = (DislocatorActivity)getActivity();
        if (activity.driveId != null) {
            Log.i(TAG, "Import from google drive map.");
            DriveId driveId = DriveId.decodeFromString(activity.driveId);
            DriveFile file = driveId.asDriveFile();
            switch (activity.import_to) {
                case REMOTE:
                    file.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null)
                            .setResultCallback(openRemoteKml);
                    activity.driveId = null; // clear id
                    break;
                case LOCAL:
                    file.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null)
                            .setResultCallback(openLocalKml);
                    activity.driveId = null; // clear id
                    break;
                case REMOTE_AND_LOCAL:
                    file.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null)
                            .setResultCallback(openRemoteAndLocalKml);
                    activity.driveId = null; // clear id
                    break;
            }
        }


        if (activity.sdCardFile != null) { // if we just imported something
            if (activity.import_to == IMPORT_TO.REMOTE || activity.import_to == IMPORT_TO.REMOTE_AND_LOCAL)
                animateCameraToCluster(mRemoteClusterManager, mRemoteMap);
            if (activity.import_to == IMPORT_TO.LOCAL || activity.import_to == IMPORT_TO.REMOTE_AND_LOCAL)
                animateCameraToCluster(mLocalClusterManager, mLocalMap);
            // reset sd card pointer
            activity.sdCardFile = null;
        }

    }

    ResultCallback<DriveApi.DriveContentsResult> openLocalKml =
            new ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(@NonNull DriveApi.DriveContentsResult driveContentsResult) {
                    if (!driveContentsResult.getStatus().isSuccess()) {
                        Toast.makeText(getContext(), getString(R.string.cannot_open_file), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    DriveContents contents = driveContentsResult.getDriveContents();
                    try {
                        readKMLItems(contents.getInputStream(), mLocalMap, mLocalClusterManager);
                        animateCameraToCluster(mLocalClusterManager, mLocalMap); // animate camera to cluster (does nothing if empty cluster)
                    } catch (FileNotFoundException e) {
                        Log.w(TAG, e.toString());
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                    } catch (XmlPullParserException e) {
                        Log.e(TAG, e.toString());
                    }
                }
            };

    ResultCallback<DriveApi.DriveContentsResult> openRemoteKml =
            new ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(@NonNull DriveApi.DriveContentsResult driveContentsResult) {
                    if (!driveContentsResult.getStatus().isSuccess()) {
                        Toast.makeText(getContext(), getString(R.string.cannot_open_file), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    DriveContents contents = driveContentsResult.getDriveContents();
                    try {
                        readKMLItems(contents.getInputStream(), mRemoteMap, mRemoteClusterManager);
                        animateCameraToCluster(mRemoteClusterManager, mRemoteMap); // animate to cluster (this checks if there are markers)
                    } catch (FileNotFoundException e) {
                        Log.w(TAG, e.toString());
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                    } catch (XmlPullParserException e) {
                        Log.e(TAG, e.toString());
                    }
                }
            };

    ResultCallback<DriveApi.DriveContentsResult> openRemoteAndLocalKml =
            new ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(@NonNull DriveApi.DriveContentsResult driveContentsResult) {
                    if (!driveContentsResult.getStatus().isSuccess()) {
                        Toast.makeText(getContext(), getString(R.string.cannot_open_file), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    DriveContents contents = driveContentsResult.getDriveContents();
                    InputStream is = contents.getInputStream();
                    try {
                        readKMLItems(is, mRemoteMap, mRemoteClusterManager);
                        animateCameraToCluster(mRemoteClusterManager, mRemoteMap); // animate to cluster (this checks if there are markers)
                        readKMLItems(is, mLocalMap, mLocalClusterManager);
                        animateCameraToCluster(mLocalClusterManager, mLocalMap); // animate camera to cluster (does nothing if empty cluster)
                    } catch (FileNotFoundException e) {
                        Log.w(TAG, e.toString());
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                    } catch (XmlPullParserException e) {
                        Log.e(TAG, e.toString());
                    }
                }
            };


    @Override
    public void onStreetViewPanoramaReady(StreetViewPanorama streetViewPanorama) {
        LatLng pos = null;
        if (mStreetViewActiveLocal) {
            pos = new LatLng(mLocalMap.getCameraPosition().target.latitude, mLocalMap.getCameraPosition().target.longitude);
            // zoom back 1 level on map so buttons work correctly
            mLocalMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    pos,
                    mLocalMap.getCameraPosition().zoom - 1));
        }
        else {
            pos = new LatLng(mRemoteMap.getCameraPosition().target.latitude, mRemoteMap.getCameraPosition().target.longitude);
            // zoom back 1 level on map so buttons work correctly
            mRemoteMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    pos,
                    mRemoteMap.getCameraPosition().zoom - 1));
        }

        streetViewPanorama.setPosition(pos);
    }


    private void enterStreetView(int map_fragment_id, boolean onLocalMap) {

        if (mStreetViewActiveLocal || mStreetViewActiveRemote)
            return;

        mStreetViewActiveLocal = onLocalMap;
        mStreetViewActiveRemote = !onLocalMap;

        SupportStreetViewPanoramaFragment sm = SupportStreetViewPanoramaFragment.newInstance();
        sm.getStreetViewPanoramaAsync(this);
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        transaction
                .add(map_fragment_id, sm)
                .addToBackStack(getString(R.string.tag_fragment_streetview))
                .commit();

        final Button btnExitStreetView = new Button(getContext());
        btnExitStreetView.setText("Exit");
        btnExitStreetView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        final ViewGroup layout = (ViewGroup) getActivity().findViewById(map_fragment_id);
        layout.addView(btnExitStreetView);

        btnExitStreetView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStack();
                layout.removeView(btnExitStreetView);
                mStreetViewActiveLocal = false;
                mStreetViewActiveRemote = false;
            }
        });

    }

    private LatLng getLocalReferenceLatLng() {

        SharedPreferences sharedPref = getActivity().getPreferences(DislocatorApplication.getAppContext().MODE_PRIVATE);

        double dLocalRefLat = getDouble(sharedPref, getResources().getString(R.string.local_ref_lat), -1000.0);
        double dLocalRefLng = getDouble(sharedPref, getResources().getString(R.string.local_ref_lng), -1000.0);
        if (dLocalRefLat != -1000.0 || dLocalRefLng != -1000.0) {
            return new LatLng(dLocalRefLat, dLocalRefLng);
        }

        return null;
    }

    private LatLng getRemoteReferenceLatLng() {

        SharedPreferences sharedPref = getActivity().getPreferences(DislocatorApplication.getAppContext().MODE_PRIVATE);

        double dRemoteRefLat = getDouble(sharedPref, getResources().getString(R.string.remote_ref_lat), -1000.0);
        double dRemoteRefLng = getDouble(sharedPref, getResources().getString(R.string.remote_ref_lng), -1000.0);
        if (dRemoteRefLat != -1000 || dRemoteRefLng != -1000) {
            return new LatLng(dRemoteRefLat, dRemoteRefLng);
        }

        return null;
    }

    private void createLocalReferenceMarker(final LatLng ref, boolean animateCamera) {
        // update local map
        if (mLocalMap != null) {
            if (mLocalReferenceMarker != null) {
                mLocalReferenceMarker.remove();
            }

            if (animateCamera) {
                mLocalMap.animateCamera(CameraUpdateFactory.
                        newLatLngZoom(ref, mLocalMap.getCameraPosition().zoom), 1000, new GoogleMap.CancelableCallback() {

                    @Override
                    public void onFinish() {
                        mLocalReferenceMarker = mLocalMap.addMarker(new MarkerOptions()
                                .position(ref)
                                .title("Local Reference Point")
                                .icon(BitmapDescriptorFactory.defaultMarker(4))
                                .draggable(false)
                        );
                        mLocalReferenceMarker.showInfoWindow();
                    }
                    @Override
                    public void onCancel() {
                    }
                });

            } else {
                mLocalReferenceMarker = mLocalMap.addMarker(new MarkerOptions()
                        .position(ref)
                        .title("Local Reference Point")
                        .icon(BitmapDescriptorFactory.defaultMarker(4))
                        .draggable(false)
                );
            }

            // save reference marker
            SharedPreferences sharedPref = getActivity().getPreferences(DislocatorApplication.getAppContext().MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            putDouble(editor, getResources().getString(R.string.local_ref_lat), ref.latitude);
            putDouble(editor, getResources().getString(R.string.local_ref_lng), ref.longitude);
            editor.commit();
        }

    }

    private void createRemoteReferenceMarker(final LatLng ref, boolean animateCamera) {
        // update remote map
        if (mRemoteMap != null) {
            if (mRemoteReferenceMarker != null) {
                mRemoteReferenceMarker.remove();
            }

            if (animateCamera) {
                mRemoteMap.animateCamera(CameraUpdateFactory.
                        newLatLngZoom(ref, mRemoteMap.getCameraPosition().zoom), 1000, new GoogleMap.CancelableCallback() {
                    @Override
                    public void onFinish() {
                        mRemoteReferenceMarker = mRemoteMap.addMarker(new MarkerOptions()
                                .position(ref)
                                .title("Remote Reference Point")
                                .icon(BitmapDescriptorFactory.defaultMarker(4))
                                .draggable(false)
                        );
                        mRemoteReferenceMarker.showInfoWindow();
                    }
                    @Override
                    public void onCancel() {}
                });
            } else {
                mRemoteReferenceMarker = mRemoteMap.addMarker(new MarkerOptions()
                        .position(ref)
                        .title("Remote Reference Point")
                        .icon(BitmapDescriptorFactory.defaultMarker(4))
                        .draggable(false)
                );
            }

            // save reference marker
            SharedPreferences sharedPref = getActivity().getPreferences(DislocatorApplication.getAppContext().MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            putDouble(editor, getResources().getString(R.string.remote_ref_lat), ref.latitude);
            putDouble(editor, getResources().getString(R.string.remote_ref_lng), ref.longitude);
            editor.commit();
        }
    }


    private void animateCameraToCluster(ClusterManager<Bone> manager, final GoogleMap map) {

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
            map.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(bounds, 100),
                    new GoogleMap.CancelableCallback() {
                        @Override
                        public void onFinish() {
                            // zoome out a little again after the bounds
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                    bounds.getCenter(), map.getCameraPosition().zoom - 2
                            ));
                        }
                        @Override
                        public void onCancel() {
                        }
                    });

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
                    Log.e(TAG, e.toString());
                }
            }
        });

    }


    public void addBone(final ClusterManager manager) {
        if (mLastKnownLocation != null) {
            LatLng point = new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
            addBone(manager, point);
        }
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
                .setTitle(getResources().getString(R.string.dialog_add_bone))
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
        Log.d(TAG, "createLocationRequest");

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @SuppressWarnings({"MissingPermission"})
    protected void startLocationUpdates() {
        if (mLocationAccessGranted) {
            Log.d(TAG, "startLocationUpdates");
            mRemoteLocationSource = new MapsFragment.RemoteLocationSource();
            mRemoteLocationSource.onResume();

            if (mLocalMap != null && !mLocalMap.isMyLocationEnabled())
                enableMyLocationLocalMap();
            if (mRemoteMap != null && !mRemoteMap.isMyLocationEnabled())
                enableMyLocationRemoteMap();

            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);

        }
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

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
        LatLng latLngLocal = new LatLng(location.getLatitude(), location.getLongitude());
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

        if (mLastKnownLocation != null) {
            Log.v(TAG, "onLocationChanged: " + location.toString());

            // update local map
            if (mLocalMap != null) {

                if (!mLocalMap.isMyLocationEnabled()) {
                    enableMyLocationLocalMap();
                }

                // check if we need to create the default reference point (if user did not set one yet)
                if (getLocalReferenceLatLng() == null) {
                    createLocalReferenceMarker(latLngLocal, true);
                }

                // check if we should auto center
                else if (mSharedPref.getBoolean(getString(R.string.pref_center_local_map), false)) {
                    mLocalMap.animateCamera(CameraUpdateFactory.
                            newLatLngZoom(
                                    new LatLng(location.getLatitude(), location.getLongitude()),
                                    mLocalMap.getCameraPosition().zoom));
                }
            }


            if (mRemoteMap != null) {
                // check if we need to create the default reference point (if user did not set one yet)
                if (getRemoteReferenceLatLng() == null) {
                    createRemoteReferenceMarker(mLatLngDefaultRemoteReference, true);
                }
            }

            // update remote map
            if (mRemoteMap != null && mRemoteLocationSource != null && mLocalReferenceMarker != null && mRemoteReferenceMarker != null) {

                if (!mRemoteMap.isMyLocationEnabled()) {
                    enableMyLocationRemoteMap();
                }

                mRemoteMap.setLocationSource(mRemoteLocationSource);
                double latDiff = location.getLatitude() - mLocalReferenceMarker.getPosition().latitude;
                double lngDiff = location.getLongitude() - mLocalReferenceMarker.getPosition().longitude;
                Location remoteLocation = new Location("Remote Fix");
                remoteLocation.setLatitude(mRemoteReferenceMarker.getPosition().latitude + latDiff);
                remoteLocation.setLongitude(mRemoteReferenceMarker.getPosition().longitude + lngDiff);
                if (mRemoteLocationSource.mListener != null)
                    mRemoteLocationSource.mListener.onLocationChanged(remoteLocation);

                // check if we should auto center
                if (mSharedPref.getBoolean(getString(R.string.pref_center_remote_map), true)) {
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

        if (mLocationAccessGranted && mLocalMap != null) {
            Log.d(TAG, "enableMyLocationLocalMap");
            mLocalMap.setMyLocationEnabled(true);
            mLocalMap.setOnMyLocationButtonClickListener(this);
        }
    }

    @SuppressWarnings({"MissingPermission"})
    private void enableMyLocationRemoteMap() {
        if (mLocationAccessGranted && mRemoteMap != null) {
            Log.d(TAG, "enableMyLocationRemoteMap");
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
            mLocationAccessGranted = mainActivity.requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, requestCode);
            return mLocationAccessGranted;

        }
        return false;

    }

    @Override
    public void onPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case DislocatorApplication.REQUEST_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onPermissionResult");
                    mLocationAccessGranted = true;
                    onMapsReady();
                }
                break;
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
