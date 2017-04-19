package com.stahlnow.android.dislocator;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class DislocatorApplication extends Application {

    private static Context context;

    public void onCreate() {
        super.onCreate();
        DislocatorApplication.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return DislocatorApplication.context;
    }

    public static boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }
}
