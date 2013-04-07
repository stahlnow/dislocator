package com.stahlnow.dislocator;

import java.io.File;
import org.mapsforge.android.AndroidUtils;
import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapScaleBar;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.overlay.ListOverlay;
import org.mapsforge.android.maps.overlay.MyLocationOverlay;
import org.mapsforge.android.maps.overlay.Marker;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.map.reader.header.FileOpenResult;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * An application which demonstrates how to use two MapView instances at the same time.
 */
public class DislocatorActivity extends MapActivity {
	
	private static final String TAG = "DislocatorActivity";
	
	private static final File MAP_FILE_LONDON = new File(Environment.getExternalStorageDirectory().getPath(), "greater-london.map");
	private static final File MAP_FILE_ZURICH = new File(Environment.getExternalStorageDirectory().getPath(), "zurich.map");
	private static final File MAP_FILE_ICELAND = new File(Environment.getExternalStorageDirectory().getPath(), "iceland.map");
	
	private static final String PREFS_IS_WALKING = "is_walking";
	private static final String PREFS_LOCAL_START_LATITUDE = "local_start_latitude";
	private static final String PREFS_LOCAL_START_LONGITUDE = "local_start_longitude";
	private static final String PREFS_REMOTE_START_LATITUDE = "remote_start_latitude";
	private static final String PREFS_REMOTE_START_LONGITUDE = "remote_start_longitude";
	
	private SharedPreferences preferences;
	
	private GeoPoint remoteStartLocation = null;
	private GeoPoint localStartLocation = null;
	
	private MyLocationOverlay myLocationOverlay;
	private MyRemoteLocationOverlay myRemoteLocationOverlay;
	
	
	ListOverlay localStartOverlay = new ListOverlay();
	Marker localStartMarker = null;
	
	ListOverlay remoteStartOverlay = new ListOverlay();
	Marker remoteStartMarker = null;
	
	
	private boolean isWalking = false;
	
	private ScreenshotCapturer screenshotCapturer;

	public MapView remoteMap;
	public MapView localMap;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.localMap = createMapView(false, MAP_FILE_ZURICH);
		this.remoteMap = createMapView(false, MAP_FILE_LONDON);
		
		this.screenshotCapturer = new ScreenshotCapturer(this);
		this.screenshotCapturer.start();
		
		// get preferences
		preferences = getApplicationContext().getSharedPreferences(TAG, MODE_PRIVATE);
		isWalking = preferences.getBoolean(PREFS_IS_WALKING, false);
		remoteStartLocation = new GeoPoint(
				preferences.getFloat(PREFS_REMOTE_START_LATITUDE, 51.51698f),
				preferences.getFloat(PREFS_REMOTE_START_LONGITUDE, -0.06438f));
		localStartLocation = new GeoPoint(
				preferences.getFloat(PREFS_LOCAL_START_LATITUDE, 0.0f),
				preferences.getFloat(PREFS_LOCAL_START_LONGITUDE, 0.0f));
		// create default remote location starting marker
        remoteStartMarker = createMarker(R.drawable.marker_red, remoteStartLocation);
		remoteStartOverlay.getOverlayItems().add(remoteStartMarker);
		this.remoteMap.getOverlays().add(remoteStartOverlay);
        
		// add my location marker to local map
		Drawable drawable = getResources().getDrawable(R.drawable.marker_green);
		drawable = Marker.boundCenterBottom(drawable);
		this.myLocationOverlay = new MyLocationOverlay(this, this.localMap, drawable);
		this.myLocationOverlay.enableMyLocation(true);
		this.myLocationOverlay.setSnapToLocationEnabled(true);
		this.localMap.setClickable(false);
		this.localMap.getOverlays().add(this.myLocationOverlay);
        
		this.remoteMap.getMapViewPosition().setZoomLevel((byte) 18);
		this.localMap.getMapViewPosition().setZoomLevel((byte) 18);
		
		// center local map on current location
		gotoLastKnownPosition();
		
		// center remote map on remote starting point
		this.remoteMap.getMapViewPosition().setCenter(remoteStartLocation); 


		// create a LineaLayout that contains both MapViews
		LinearLayout linearLayout = new LinearLayout(this);

		// if the device orientation is portrait, change the orientation to vertical
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			linearLayout.setOrientation(LinearLayout.VERTICAL);
		}

		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT, 1);
		this.localMap.setLayoutParams(layoutParams);
		this.remoteMap.setLayoutParams(layoutParams);

		// add both MapViews to the LinearLayout
		linearLayout.addView(this.remoteMap);
		linearLayout.addView(this.localMap);
		setContentView(linearLayout);
		
		// if user was walking, restart walk mode
		if (isWalking) {
			localStartMarker = createMarker(R.drawable.marker_red, localStartLocation);
			localStartOverlay.getOverlayItems().add(localStartMarker);
			localMap.getOverlays().add(localStartOverlay);
			// start remote marker tracking
			Drawable drawable2 = getResources().getDrawable(R.drawable.marker_green);
			drawable = Marker.boundCenterBottom(drawable2);
			myRemoteLocationOverlay = new MyRemoteLocationOverlay(this, remoteMap, drawable2, remoteStartLocation, localStartLocation);
			myRemoteLocationOverlay.enableMyLocation(true);
			myRemoteLocationOverlay.setSnapToLocationEnabled(true);
			remoteMap.setClickable(false);
			remoteMap.getOverlays().add(myRemoteLocationOverlay);
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		this.screenshotCapturer.interrupt();
	}
	
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.walk).setTitle(isWalking? "Stop walk" : "Start a walk");
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		
		case R.id.walk:
			
			if (!isWalking) {
				
				if (remoteStartLocation == null) {
					showToastOnUiThread("Set remote location first.");
				}
				
				else {
				
					setWalking(true);
					
					// create local start marker
					localStartLocation = new GeoPoint(0.0,0.0);
					localStartLocation = localMap.getMapViewPosition().getMapPosition().geoPoint;
					localStartMarker = createMarker(R.drawable.marker_red, localStartLocation);
					localStartOverlay.getOverlayItems().add(localStartMarker);
					localMap.getOverlays().add(localStartOverlay);
					
					// save localStartLocation to preferences to get it later if the user quits the application
					preferences = getApplicationContext().getSharedPreferences(TAG, MODE_PRIVATE);
					SharedPreferences.Editor editor = preferences.edit();
					editor.putFloat(PREFS_LOCAL_START_LATITUDE, (float)localStartLocation.latitude);
					editor.putFloat(PREFS_LOCAL_START_LONGITUDE, (float)localStartLocation.longitude);
					editor.commit();
					
					// start remote marker tracking
					Drawable drawable = getResources().getDrawable(R.drawable.marker_green);
					drawable = Marker.boundCenterBottom(drawable);
					myRemoteLocationOverlay = new MyRemoteLocationOverlay(this, remoteMap, drawable, remoteStartLocation, localStartLocation);
					myRemoteLocationOverlay.enableMyLocation(true);
					myRemoteLocationOverlay.setSnapToLocationEnabled(true);
					remoteMap.setClickable(false);
					remoteMap.getOverlays().add(myRemoteLocationOverlay);
				}
				
				return true;
				
			} else {
				
				try {
					// try remove local start marker
					localStartOverlay.getOverlayItems().remove(localStartMarker);
					localMap.getOverlays().remove(localStartOverlay);
				
					// stop remote marker tracking
					myRemoteLocationOverlay.enableMyLocation(false);
					myRemoteLocationOverlay.setSnapToLocationEnabled(false);
					remoteMap.setClickable(true);
					remoteMap.getOverlays().remove(myRemoteLocationOverlay);
				} catch (Exception e) {
					; // we don't care
				}
				
				setWalking(false);
				
				return true;
				
			}
			
		case R.id.locate:
			gotoLastKnownPosition();
			return true;
			
		case R.id.set_remote_location:
			
			if (!isWalking) {
			
				remoteStartLocation = new GeoPoint(0.0, 0.0);
				remoteStartLocation = remoteMap.getMapViewPosition().getCenter();
				
				try {
					// try remove current remote start marker
					remoteStartOverlay.getOverlayItems().remove(remoteStartMarker);
					remoteMap.getOverlays().remove(remoteStartOverlay);
				} catch (Exception e) {
					; // we don't care
				}
				
				remoteStartMarker = createMarker(R.drawable.marker_red, remoteStartLocation);
				remoteStartOverlay.getOverlayItems().add(remoteStartMarker);
				remoteMap.getOverlays().add(remoteStartOverlay);
				remoteMap.redraw();
				
				// save remoteStartLocation to preferences to get it later if the user quits the application
				preferences = getApplicationContext().getSharedPreferences(TAG, MODE_PRIVATE);
				SharedPreferences.Editor editor = preferences.edit();
				editor.putFloat(PREFS_REMOTE_START_LATITUDE, (float)remoteStartLocation.latitude);
				editor.putFloat(PREFS_REMOTE_START_LONGITUDE, (float)remoteStartLocation.longitude);
				editor.commit();
	
				return true;
			}
			else {
				showToastOnUiThread("Stop the walk first.");
				return true;
			}
			
		case R.id.set_remote_map:
			return true;
		case R.id.set_remote_london:
			this.remoteMap.setMapFile(MAP_FILE_LONDON);
			remoteStartLocation = null;
			return true;
		case R.id.set_remote_zurich:
			this.remoteMap.setMapFile(MAP_FILE_ZURICH);
			remoteStartLocation = null;
			return true;
		case R.id.set_remote_iceland:
			this.remoteMap.setMapFile(MAP_FILE_ICELAND);
			remoteStartLocation = null;
			return true;
			
		case R.id.set_local_map:
			return true;
		case R.id.set_local_london:
			this.localMap.setMapFile(MAP_FILE_LONDON);
			localStartLocation = null;
			return true;
		case R.id.set_local_zurich:
			this.localMap.setMapFile(MAP_FILE_ZURICH);
			localStartLocation = null;
			return true;
		case R.id.set_local_iceland:
			this.localMap.setMapFile(MAP_FILE_ICELAND);
			localStartLocation = null;
			return true;
		
		case R.id.make_screenshot_png:
			this.screenshotCapturer.captureScreenshot(CompressFormat.PNG);
			return true;

		case R.id.set_preferences:
			//startActivity(new Intent(this, EditPreferences.class));
			return true;

		default:
			return false;
		}
	}
	
	private void setWalking(boolean walking) {
		preferences = getApplicationContext().getSharedPreferences(TAG, MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit(); 
		editor.putBoolean(PREFS_IS_WALKING, walking);
		editor.commit();
		isWalking = walking;
	}
	
	

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			finish();
			return true;
		}
		// forward the event to both MapViews for simultaneous movement
		return this.localMap.onKeyDown(keyCode, event) | this.remoteMap.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// forward the event to both MapViews for simultaneous movement
		return this.localMap.onKeyUp(keyCode, event) | this.remoteMap.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		// forward the event to both MapViews for simultaneous movement
		return this.localMap.onTrackballEvent(event) | this.remoteMap.onTrackballEvent(event);
	}

	private MapView createMapView(boolean imperialUnits, File f) {
		MapView mapView = new MapView(this);
		mapView.setClickable(true);
		mapView.setBuiltInZoomControls(true);
		mapView.getMapMover().setMoveSpeedFactor(1);

		MapScaleBar mapScaleBar = mapView.getMapScaleBar();
		mapScaleBar.setImperialUnits(imperialUnits);
		mapScaleBar.setShowMapScaleBar(true);

		FileOpenResult fileOpenResult = mapView.setMapFile(f);
		if (!fileOpenResult.isSuccess()) {
			Toast.makeText(this, fileOpenResult.getErrorMessage(), Toast.LENGTH_LONG).show();
			finish();
		}

		return mapView;
	}
	
	
	
	
	private Marker createMarker(int resourceIdentifier, GeoPoint geoPoint) {
		Drawable drawable = getResources().getDrawable(resourceIdentifier);
		return new Marker(geoPoint, Marker.boundCenterBottom(drawable));
	}

	
	/**
	 * Centers the map to the last known position as reported by the most accurate location provider. If the last
	 * location is unknown, a toast message is displayed instead.
	 */
	private void gotoLastKnownPosition() {
		Location bestLocation = null;
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		for (String provider : locationManager.getProviders(true)) {
			Location currentLocation = locationManager.getLastKnownLocation(provider);
			if (bestLocation == null || bestLocation.getAccuracy() > currentLocation.getAccuracy()) {
				bestLocation = currentLocation;
			}
		}

		// check if a location has been found
		if (bestLocation != null) {
			GeoPoint geoPoint = new GeoPoint(bestLocation.getLatitude(), bestLocation.getLongitude());
			this.localMap.getMapViewPosition().setCenter(geoPoint);
		} else {
			showToastOnUiThread("Could not get location fix...");
		}
	}
	
	/**
	 * Uses the UI thread to display the given text message as toast notification.
	 * 
	 * @param text
	 *            the text message to display
	 */
	void showToastOnUiThread(final String text) {
		if (AndroidUtils.currentThreadIsUiThread()) {
			Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
			toast.show();
		} else {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast toast = Toast.makeText(DislocatorActivity.this, text, Toast.LENGTH_LONG);
					toast.show();
				}
			});
		}
	}
}
