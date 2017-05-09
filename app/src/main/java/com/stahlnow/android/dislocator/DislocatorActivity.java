package com.stahlnow.android.dislocator;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;


public class DislocatorActivity extends AppCompatActivity  {

    private static final String TAG = DislocatorActivity.class.getSimpleName();

    private DrawerLayout mDrawerLayout;
    private Toolbar mToolbar;
    private ActionBarDrawerToggle mDrawerToggle;

    private PermissionResultListener mPermissionResultListener;

    // Activity member variables
    public Bundle mSettings;
    public String driveIdLocal = null;
    public String driveIdRemote = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettings = new Bundle();

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        setContentView(R.layout.activity_dislocator);

        // Setup Actionbar / Toolbar
        configureToolbar();

        // Setup Navigation Drawer Layout
        configureNavigationDrawer();

        // Start Maps Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content, new MapsFragment(), getString(R.string.tag_fragment_maps)).commit();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(getString(R.string.tag_fragment_import));
        if (fragment != null)
        {
            ((ImportFragment)fragment).onActivityResult(requestCode, resultCode,data);
        }
    }


    @Override
    public void onBackPressed() {
        // because of google maps bug, we always show the map instead of using popBackStack() function

        // exit if map is shown
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(getString(R.string.tag_fragment_maps));
        if (fragment != null && fragment.isVisible()) {
            finish();
        }

        // or else show the map
        else {

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content, new MapsFragment(), getResources().getString(R.string.tag_fragment_maps))
                    .addToBackStack(getString(R.string.tag_fragment_maps))
                    .commit();
        }

        // what we normally do
        /*
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            //getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
        */
    }

    private void configureToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.action_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        //getSupportActionBar().setLogo(R.mipmap.ic_launcher);
    }

    private void configureNavigationDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                mToolbar,
                R.string.drawer_open,
                R.string.drawer_close
        ) {

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                //getSupportActionBar().setTitle(mTitle);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                //getSupportActionBar().setTitle(mDrawerTitle);
            }
        };


        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.addDrawerListener(mDrawerToggle);


        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {

                //Checking if the item is in checked state or not, if not make it in checked state
                //if(menuItem.isChecked()) menuItem.setChecked(false);
                //else menuItem.setChecked(true);

                //Closing drawer on item click
                mDrawerLayout.closeDrawers();

                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

                //Check to see which item was being clicked and perform appropriate action
                switch (menuItem.getItemId()){

                    case R.id.menu_maps:
                        transaction
                                .replace(R.id.content, new MapsFragment(), getResources().getString(R.string.tag_fragment_maps))
                                .addToBackStack(getString(R.string.tag_fragment_maps))
                                .commit();
                        return true;

                    case R.id.menu_import:
                        transaction
                                .replace(R.id.content, new ImportFragment(), getResources().getString(R.string.tag_fragment_import))
                                .addToBackStack(getString(R.string.tag_fragment_import))
                                .commit();
                        return true;

                    case R.id.menu_export:
                        transaction
                                .replace(R.id.content,  new ExportFragment(), getResources().getString(R.string.tag_fragment_export))
                                .addToBackStack(getString(R.string.tag_fragment_export))
                                .commit();
                        return true;

                    case R.id.menu_settings:
                        transaction
                                .replace(R.id.content, new SettingsFragment(), getResources().getString(R.string.tag_fragment_settings))
                                .addToBackStack(getString(R.string.tag_fragment_settings))
                                .commit();
                        return true;

                    default:
                        return true;
                }

            }
        });
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState(); // calling sync state is necessary or else the hamburger icon wont show up
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        switch(itemId) {
            // Android home
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }

        return false;
    }


    public void setPermissionResultListener(PermissionResultListener mPermissionResultListener) {
        this.mPermissionResultListener = mPermissionResultListener;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (mPermissionResultListener != null) {
            mPermissionResultListener.onPermissionResult(requestCode, permissions, grantResults);
        }
    }

    public boolean requestPermission(String permission, int requestCode) {

        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted: " + permission);
                return true;
            } else {
                Log.v(TAG,"Permission is revoked: " + permission);
                android.support.v4.app.ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
                return false;
            }
        }
        else { // permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted");
            return true;
        }

    }


}

