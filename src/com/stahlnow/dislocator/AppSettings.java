package com.stahlnow.dislocator;

import org.mapsforge.core.model.LatLong;

import android.content.Context;
import android.content.SharedPreferences;

public class AppSettings {
	
	public static final String DISLOCATOR_PREF_NAME = "com.stahlnow.dislocator";
	
	public static final String LOCAL_START_LATITUDE = "localStartLat";
	public static final String LOCAL_START_LONGITUDE = "localStartLng";
	
	public static final String REMOTE_START_LATITUDE = "remoteStartLat";
	public static final String REMOTE_START_LONGITUDE = "remoteStartLng";
	
	public static final String REMOTE_MAP = "remoteMap";
	public static final String LOCAL_MAP = "localMap";
	
	public static final String SERVICE_STATE = "isServiceRunning";
	public static final String LOGGING_INTERVAL = "loggingInterval";
	public static final String KML_FILE = "kmlFile";
	
	public static LatLong getLocalStartPoint(Context context) {
		
		SharedPreferences pref = context.getSharedPreferences(DISLOCATOR_PREF_NAME, 0);
				
		return new LatLong(
				pref.getFloat(LOCAL_START_LATITUDE, 0.0f),
				pref.getFloat(LOCAL_START_LONGITUDE, 0.0f));
	}
	
	public static void setLocalStartPoint(Context context, LatLong point) {
		SharedPreferences pref = context.getSharedPreferences(DISLOCATOR_PREF_NAME, 0);
		SharedPreferences.Editor editor = pref.edit();
		editor.putFloat(LOCAL_START_LATITUDE, (float)point.latitude);
		editor.putFloat(LOCAL_START_LONGITUDE, (float)point.longitude);
		editor.commit();
	}
	
	public static LatLong getRemoteStartPoint(Context context) {
		
		SharedPreferences pref = context.getSharedPreferences(DISLOCATOR_PREF_NAME, 0);
				
		return new LatLong(
				pref.getFloat(REMOTE_START_LATITUDE, 51.51698f),
				pref.getFloat(REMOTE_START_LONGITUDE, -0.06438f));
	}
	
	public static void setRemoteStartPoint(Context context, LatLong point) {
		SharedPreferences pref = context.getSharedPreferences(DISLOCATOR_PREF_NAME, 0);
		SharedPreferences.Editor editor = pref.edit();
		editor.putFloat(REMOTE_START_LATITUDE, (float)point.latitude);
		editor.putFloat(REMOTE_START_LONGITUDE, (float)point.longitude);
		editor.commit();
	}
	
	public static String getRemoteMap(Context context) {
		SharedPreferences pref = context.getSharedPreferences(DISLOCATOR_PREF_NAME, 0);
		return pref.getString(REMOTE_MAP, "greater-london.map");
	}
	
	public static void setRemoteMap(Context context, String map) {
		SharedPreferences pref = context.getSharedPreferences(DISLOCATOR_PREF_NAME, 0);
		SharedPreferences.Editor editor = pref.edit();
		editor.putString(REMOTE_MAP, map);
		editor.commit();
	}
	
	public static String getLocalMap(Context context) {
		SharedPreferences pref = context.getSharedPreferences(DISLOCATOR_PREF_NAME, 0);
		return pref.getString(LOCAL_MAP, "iceland.map");
	}
	
	public static void setLocalMap(Context context, String map) {
		SharedPreferences pref = context.getSharedPreferences(DISLOCATOR_PREF_NAME, 0);
		SharedPreferences.Editor editor = pref.edit();
		editor.putString(LOCAL_MAP, map);
		editor.commit();
	}
	
	public static boolean getServiceRunning(Context context){
		SharedPreferences pref = context.getSharedPreferences(DISLOCATOR_PREF_NAME, 0);
		
		return pref.getBoolean(SERVICE_STATE, false);
	}
	
	public static void setServiceRunning(Context context, boolean isRunning){
		SharedPreferences pref = context.getSharedPreferences(DISLOCATOR_PREF_NAME, 0);
		SharedPreferences.Editor editor = pref.edit();

		editor.putBoolean(SERVICE_STATE, isRunning);
		editor.commit();
	}
	
	public static int getLoggingInterval(Context context){
		SharedPreferences pref = context.getSharedPreferences(DISLOCATOR_PREF_NAME, 0);
		
		return pref.getInt(LOGGING_INTERVAL, 5);
	}
	
	public static void setLoggingInterval(Context context, int interval){
		SharedPreferences pref = context.getSharedPreferences(DISLOCATOR_PREF_NAME, 0);
		SharedPreferences.Editor editor = pref.edit();

		editor.putInt(LOGGING_INTERVAL, interval);
		editor.commit();
	}
	
	public static String getKMLFileName(Context context){
		SharedPreferences pref = context.getSharedPreferences(DISLOCATOR_PREF_NAME, 0);
		
		return pref.getString(KML_FILE, "");
	}
	
	public static void setKMLFileName(Context context,String filename){
		SharedPreferences pref = context.getSharedPreferences(DISLOCATOR_PREF_NAME, 0);
		SharedPreferences.Editor editor = pref.edit();

		editor.putString(KML_FILE, filename);
		editor.commit();
	}

}
