package com.stahlnow.android.dislocator;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.preference.Preference;
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

        Preference button = findPreference(getString(R.string.legal_notices));
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                // DialogFragment.show() will take care of adding the fragment
                // in a transaction.  We also want to remove any currently showing
                // dialog, so make our own transaction and take care of that here.
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                Fragment prev = getActivity().getSupportFragmentManager().findFragmentByTag(getString(R.string.tag_fragment_google_license_dialog));
                if (prev != null) {
                    transaction.remove(prev);
                }
                transaction.addToBackStack(null);

                // Create and show the dialog.
                DialogFragment dialog = new GoogleLicenseDialogFragment(); // or .newInstance()
                dialog.show(transaction, getString(R.string.tag_fragment_google_license_dialog));

                return true;
            }
        });

        // kept for reference
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
