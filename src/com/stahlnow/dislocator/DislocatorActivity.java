package com.stahlnow.dislocator;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.layer.MyLocationOverlay;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.renderer.TileRendererLayer;

import com.stahlnow.dislocator.Bones.Bone;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

/**
 * An application which demonstrates how to use two MapView instances at the same time.
 */
public class DislocatorActivity extends Activity {
	
	private static final String TAG = "DislocatorActivity";
	
	private static final String MAP_FILE_LONDON = "greater-london.map";
	private static final String MAP_FILE_ZURICH = "zurich.map";
	private static final String MAP_FILE_ICELAND = "iceland.map";
	
	private TileCache remoteCache;
	private TileCache localCache;
	
	private TileRendererLayer remoteRenderer;
	private TileRendererLayer localRenderer;
	
	private File remoteMapFile;
	private File localMapFile;
	
	private LatLong remoteStartLocation = null;
	private LatLong localStartLocation = null;
	
	private MyLocationOverlay myLocationOverlay;
	private MyRemoteLocationOverlay myRemoteLocationOverlay;
	
	
	//ListOverlay localStartOverlay = new ListOverlay();
	Marker localStartMarker = null;
	
	//ListOverlay remoteStartOverlay = new ListOverlay();
	Marker remoteStartMarker = null;
	
	
	private boolean isWalking = false;

	public static MapView remoteMap;
	public static MapView localMap;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		remoteStartLocation = AppSettings.getRemoteStartPoint(this);
		localStartLocation = AppSettings.getLocalStartPoint(this);
		
		setMap(true, AppSettings.getRemoteMap(this)); 
		setMap(false, AppSettings.getLocalMap(this));
		
		// create remote map view
		remoteMap = new MapView(this);
		remoteMap.setClickable(true);
		remoteMap.getMapScaleBar().setVisible(true);
		remoteCache = Utils.createExternalStorageTileCache(this, getClass().getSimpleName() + "/local");
		remoteRenderer =  Utils.createTileRendererLayer(
				remoteCache,
				remoteMap.getModel().mapViewPosition,
				remoteMap.getLayerManager(),
				remoteMapFile);		
		remoteMap.getLayerManager().getLayers().add(remoteRenderer);
		
		// create remote map view
		localMap = new MapView(this);
		localMap.setClickable(true);
		localMap.getMapScaleBar().setVisible(true);
		localCache = Utils.createExternalStorageTileCache(this, getClass().getSimpleName() + "/remote");
		localRenderer =  Utils.createTileRendererLayer(
				localCache,
				localMap.getModel().mapViewPosition,
				localMap.getLayerManager(),
				localMapFile);		
		localMap.getLayerManager().getLayers().add(localRenderer);
		
		
		isWalking = AppSettings.getServiceRunning(this);
		
		// create default remote location starting marker
		remoteStartMarker = Utils.createMarker(this, R.drawable.marker_red, remoteStartLocation);
		remoteMap.getLayerManager().getLayers().add(remoteStartMarker);
		
        
		// add my location marker to local map
		Drawable drawable = getResources().getDrawable(R.drawable.marker_green);
		Bitmap bitmap = AndroidGraphicFactory.convertToBitmap(drawable);
		myLocationOverlay = new MyLocationOverlay(this, localMap.getModel().mapViewPosition, bitmap);
		myLocationOverlay.enableMyLocation(true);
		myLocationOverlay.setSnapToLocationEnabled(true);
		localMap.setClickable(false);
		localMap.getLayerManager().getLayers().add(myLocationOverlay);
        
		remoteMap.getModel().mapViewPosition.setZoomLevel((byte) 18);
		localMap.getModel().mapViewPosition.setZoomLevel((byte) 18);
		
		// center local map on current location
		gotoLastKnownPosition();
		
		// center remote map on remote starting point 
		remoteMap.getModel().mapViewPosition.setCenter(remoteStartLocation);

		// create a LineaLayout that contains both MapViews
		LinearLayout linearLayout = new LinearLayout(this);

		// if the device orientation is portrait, change the orientation to vertical
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			linearLayout.setOrientation(LinearLayout.VERTICAL);
		}

		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT, 1);
		localMap.setLayoutParams(layoutParams);
		remoteMap.setLayoutParams(layoutParams);

		// add both MapViews to the LinearLayout
		linearLayout.addView(remoteMap);
		linearLayout.addView(localMap);
		
		View zcView = LayoutInflater.from(this).inflate(R.layout.zoom_controls, null, false);
		linearLayout.addView(zcView);
		
		setContentView(linearLayout);
		
		
		ZoomControls zc = (ZoomControls) findViewById(R.id.zoomControls);
		zc.setOnZoomInClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				byte z = remoteMap.getModel().mapViewPosition.getZoomLevel();
            	z += 1;
            	if (z < 25) {
	            	remoteMap.getModel().mapViewPosition.setZoomLevel(z);
	        		localMap.getModel().mapViewPosition.setZoomLevel(z);
            	}
			}
		});
		zc.setOnZoomOutClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				byte z = remoteMap.getModel().mapViewPosition.getZoomLevel();
            	z -= 1;
            	if (z > remoteMap.getModel().mapViewPosition.getZoomLevelMin()) {
	            	remoteMap.getModel().mapViewPosition.setZoomLevel(z);
	        		localMap.getModel().mapViewPosition.setZoomLevel(z);
            	}
			}
		});
		
		// if user was walking, restart walk mode
		if (isWalking) {
			localStartMarker = Utils.createMarker(this, R.drawable.marker_red, localStartLocation);
			localMap.getLayerManager().getLayers().add(localStartMarker);
			
			// start remote marker tracking
			Drawable drawable2 = getResources().getDrawable(R.drawable.marker_green);
			Bitmap bitmap2 = AndroidGraphicFactory.convertToBitmap(drawable2);
			
			myRemoteLocationOverlay = new MyRemoteLocationOverlay(this, remoteMap.getModel().mapViewPosition, bitmap2, remoteStartLocation, localStartLocation);
			myRemoteLocationOverlay.enableMyLocation(true);
			myRemoteLocationOverlay.setSnapToLocationEnabled(true);
			remoteMap.setClickable(false);
			remoteMap.getLayerManager().getLayers().add(myRemoteLocationOverlay);
		}
		
		
		loadBones();
		
	}
	
	/*
	@Override
	protected void onResume() {
		super.onResume();
		loadBones();
	}
	*/
	
	public void loadBones() {
		
		Bones.removeAllItems();	// clear list
		
		String root = Environment.getExternalStorageDirectory().toString();
        File dir = new File(root + "/Dislocator");
        if (!dir.exists())
        	dir.mkdirs();
        File file = new File (dir, "bones.txt");


		try {
			//PrintWriter csvWriter = new  PrintWriter(new FileWriter(file, true));
		    Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
			    String line = scanner.nextLine();
			    List<String> bones = Arrays.asList(line.split(","));
			    onBoneAdded(
			    		false,
			    		bones.get(0),		// name
			    		bones.get(1),		// description
			    		new LatLong(Double.parseDouble(bones.get(4)), Double.parseDouble(bones.get(5))),	// remote
			    		new LatLong(Double.parseDouble(bones.get(2)), Double.parseDouble(bones.get(3)))		// local
			    );
			}
			
			//csvWriter.close();
			
		} catch(Exception e) { 
		    Log.e(TAG, e.toString());
		} 
				
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
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
					showToast("Set remote location first.");
				}
				
				else {
				
					setWalking(true);
					
					// create local start marker
					localStartLocation = new LatLong(0.0,0.0);
					localStartLocation = localMap.getModel().mapViewPosition.getMapPosition().latLong;
					localStartMarker = Utils.createMarker(this, R.drawable.marker_red, localStartLocation);
					localMap.getLayerManager().getLayers().add(localStartMarker);
					
					// save localStartLocation to preferences to get it later if the user quits the application
					AppSettings.setLocalStartPoint(this, new LatLong(localStartLocation.latitude, localStartLocation.longitude));
					
					// start remote marker tracking
					Drawable drawable2 = getResources().getDrawable(R.drawable.marker_green);
					Bitmap bitmap2 = AndroidGraphicFactory.convertToBitmap(drawable2);
					myRemoteLocationOverlay = new MyRemoteLocationOverlay(this, remoteMap.getModel().mapViewPosition, bitmap2, remoteStartLocation, localStartLocation);
					
					
					myRemoteLocationOverlay.enableMyLocation(true);
					myRemoteLocationOverlay.setSnapToLocationEnabled(true);
					remoteMap.setClickable(false);
					remoteMap.getLayerManager().getLayers().add(myRemoteLocationOverlay);
				}
				
				return true;
				
			} else {
				
				try {
					// try remove local start marker
					localMap.getLayerManager().getLayers().remove(localStartMarker);
				
					// stop remote marker tracking
					myRemoteLocationOverlay.enableMyLocation(false);
					myRemoteLocationOverlay.setSnapToLocationEnabled(false);
					remoteMap.setClickable(true);
					remoteMap.getLayerManager().getLayers().remove(myRemoteLocationOverlay);
				} catch (Exception e) {
					; // we don't care
				}
				
				setWalking(false);
				
				return true;
				
			}
			
		case R.id.locate:
			gotoLastKnownPosition();
			return true;
			
		case R.id.bone:
			if (isWalking) {
				addBone();
				return true;
			} else {
				showToast("Start the walk first.");
				return true;
			}
			
		case R.id.set_remote_location:
			
			if (!isWalking) {
			
				remoteStartLocation = new LatLong(0.0, 0.0);
				remoteStartLocation = remoteMap.getModel().mapViewPosition.getCenter();
				
				try {
					// try remove current remote start marker
					remoteMap.getLayerManager().getLayers().remove(remoteStartMarker);
				} catch (Exception e) {
					; // we don't care
				}
				
				remoteStartMarker = Utils.createMarker(this, R.drawable.marker_red, remoteStartLocation);
				remoteMap.getLayerManager().getLayers().add(remoteStartMarker);
				remoteMap.repaint();
				
				// save remoteStartLocation to preferences to get it later if the user quits the application
				AppSettings.setRemoteStartPoint(this, new LatLong(remoteStartLocation.latitude, remoteStartLocation.longitude));
	
				return true;
			}
			else {
				showToast("Stop the walk first.");
				return true;
			}
			
		case R.id.set_remote_map:
			return true;
		case R.id.set_remote_london:
			changeMap(true, MAP_FILE_LONDON);
			return true;
		case R.id.set_remote_zurich:
			changeMap(true, MAP_FILE_ZURICH);
			return true;
		case R.id.set_remote_iceland:
			changeMap(true, MAP_FILE_ICELAND);
			return true;
			
		case R.id.set_local_map:
			return true;
		case R.id.set_local_london:
			changeMap(false, MAP_FILE_LONDON);
			return true;
		case R.id.set_local_zurich:
			changeMap(false, MAP_FILE_ZURICH);
			return true;
		case R.id.set_local_iceland:
			changeMap(false, MAP_FILE_ICELAND);
			return true;
			
			
		case R.id.bones_list:
			Intent i = new Intent(this, BonesListActivity.class);
			startActivity(i);
			return true;
		
		case R.id.export_kml:
			if (Bones.exportKML()) {
				showToast("Success! KML files written to /sdcard/Dislocator/ directory");
			} else {
				showToast("Oooops, couldn't write KML files");
			}
			return true;

		/*	
		case R.id.set_preferences:
			//startActivity(new Intent(this, EditPreferences.class));
			return true;
		*/
		

		default:
			return false;
		}
	}
	
	private void addBone() {
		
		LayoutInflater li = LayoutInflater.from(this);
		View promptsView = li.inflate(R.layout.bone, null);

		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

		alertDialogBuilder.setView(promptsView);

		final EditText boneName = (EditText) promptsView.findViewById(R.id.boneName);
		final EditText boneDescription = (EditText) promptsView.findViewById(R.id.boneDescription);

		// set dialog message
		alertDialogBuilder
			.setCancelable(false)
			.setPositiveButton("OK",
			  new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog,int id) {
				
			    	onBoneAdded(true, boneName.getText().toString(), boneDescription.getText().toString());
				
			    }
			  })
			.setNegativeButton("Cancel",
			  new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog,int id) {
				dialog.cancel();
			    }
			  });

		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();

		// show it
		alertDialog.show();
		
		
		
		
	}
	
	private void onBoneAdded(boolean writeToFile, String name, String description) {
		
		LatLong r = null;
		LatLong l = null;
		
		if (myRemoteLocationOverlay.getLastLocation() != null) {
			r = new LatLong(
					myRemoteLocationOverlay.getLastLocation().getLatitude(),
					myRemoteLocationOverlay.getLastLocation().getLongitude());
		}
		if (myLocationOverlay.getLastLocation() != null) {
			l = new LatLong(
					myLocationOverlay.getLastLocation().getLatitude(),
					myLocationOverlay.getLastLocation().getLongitude());
		}
		
		onBoneAdded(writeToFile, name, description, r, l);
		
	}
	
	private void onBoneAdded(boolean writeToFile, String name, String description, LatLong r, LatLong l) {
		TextView bubbleView = new TextView(this);
		Utils.setBackground(bubbleView, getResources().getDrawable(R.drawable.balloon_overlay_unfocused));
		bubbleView.setGravity(Gravity.CENTER);
		bubbleView.setMaxEms(20);
		bubbleView.setTextSize(15);
		bubbleView.setTextColor(Color.BLACK);
		bubbleView.setText(name);
		Bitmap bitBone = Utils.viewToBitmap(this, bubbleView);
		
		if (r != null && l != null) {
			
			Bone b = new Bone(
					name,
					description,
					remoteMap.getModel().mapViewPosition.getCenter(),
					localMap.getModel().mapViewPosition.getCenter(),
					new Marker(r, bitBone, 0, -bitBone.getHeight() / 2),
					new Marker(l, bitBone, 0, -bitBone.getHeight() / 2)
					);
			
			remoteMap.getLayerManager().getLayers().add(b.markerRemote);
			localMap.getLayerManager().getLayers().add(b.markerLocal);
			
			
			Bones.addItem(b, writeToFile);
			
			
		} else {
			showToast("No location fix. Try again.");
		}
	}
	
	private void changeMap(boolean remote, String file) {
		if (!isWalking) {
			if (remote) {
				setMap(remote, file);
				//remoteMap.setMapFile(remoteMapFile);
				remoteRenderer.setMapFile(remoteMapFile);
				remoteStartLocation = null;
			} else {
				setMap(remote, file);
				//localMap.setMapFile(localMapFile);
				localRenderer.setMapFile(localMapFile);
				localStartLocation = null;
			}
		} else {
			showToast("Stop the walk first.");
		}
	}
	
	private void setMap(boolean remote, String file) {
		if (remote) {
			remoteMapFile = new File(Environment.getExternalStorageDirectory().getPath(), file);
			AppSettings.setRemoteMap(this, file);
		} else {
			localMapFile = new File(Environment.getExternalStorageDirectory().getPath(), file);
			AppSettings.setLocalMap(this, file);
		}
	}
	
	private void setWalking(boolean walking) {
		AppSettings.setServiceRunning(this, walking);
		isWalking = walking;
	}
	
	

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			finish();
			return true;
		}
		// forward the event to both MapViews for simultaneous movement
		return localMap.onKeyDown(keyCode, event) | remoteMap.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// forward the event to both MapViews for simultaneous movement
		return localMap.onKeyUp(keyCode, event) | remoteMap.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		// forward the event to both MapViews for simultaneous movement
		return localMap.onTrackballEvent(event) | remoteMap.onTrackballEvent(event);
	}
	
	/*
	@Override
	public boolean onTouchEvent(MotionEvent motionEvent) {
		Log.v(TAG, motionEvent.toString() + " " + motionEvent.getAction());
		return localMap.onTouchEvent(motionEvent);
	}
	*/


	/**
	 * Centers the map to the last known position as reported by the most accurate location provider. If the last
	 * location is unknown, a toast message is displayed instead.
	 */
	private LatLong gotoLastKnownPosition() {
		Location bestLocation = null;
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		for (String provider : locationManager.getProviders(true)) {
			Location currentLocation = locationManager.getLastKnownLocation(provider);
			
			if (currentLocation != null) {
				if (bestLocation == null || bestLocation.getAccuracy() > currentLocation.getAccuracy()) {
					bestLocation = currentLocation;
				}
			}
		}

		// check if a location has been found
		if (bestLocation != null) {
			LatLong l = new LatLong(bestLocation.getLatitude(), bestLocation.getLongitude());
			localMap.getModel().mapViewPosition.setCenter(l);
			return l;
		} else {
			showToast("Could not get location fix...");
			return null;
		}
		
	}
	
	
	private void toggleLogging(boolean isStart, int interval){
		AlarmManager manager = (AlarmManager)getSystemService(Service.ALARM_SERVICE);
		PendingIntent loggerIntent = PendingIntent.getBroadcast(this, 0,new Intent(this,AlarmReceiver.class), 0);
		
		if(isStart){
			manager.cancel(loggerIntent);
			
			AppSettings.setServiceRunning(this, false);
			
			Log.v(TAG, "Service Stopped.");
		}
		else{
			setKMLFileName();
			
			long duration = interval * 60 * 1000;
			
			manager.setRepeating(
					AlarmManager.ELAPSED_REALTIME_WAKEUP,
					SystemClock.elapsedRealtime(),
					duration,
					loggerIntent);
			
			AppSettings.setServiceRunning(this, true);
			
			Log.v(TAG, "Service Started with interval " + interval + ", KML filename: " + AppSettings.getKMLFileName(this));
		}
	}
	
	@SuppressLint("SimpleDateFormat")
	public void setKMLFileName(){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dateString = sdf.format(new Date());
		String filename = "Dislocator." + dateString + ".kml";
		filename = filename.replace(":", "-");
		filename = filename.replace(" ", "-");
		AppSettings.setKMLFileName(this, filename);
	}
	
	
	void showToast(final String text) {
		Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
		toast.show();
	}
}
