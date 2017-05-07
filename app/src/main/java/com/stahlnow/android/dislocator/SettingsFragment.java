package com.stahlnow.android.dislocator;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

public class SettingsFragment extends PreferenceFragmentCompat {

    private static final String TAG = SettingsFragment.class.getSimpleName();

    private SharedPreferences.OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        mOnSharedPreferenceChangeListener =
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                        // implement listener
                    }
                };
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sharedPref.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);

    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        if (mOnSharedPreferenceChangeListener != null) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            sharedPref.unregisterOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
        }
    }
    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        if (mOnSharedPreferenceChangeListener != null) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            sharedPref.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {


    }

}
