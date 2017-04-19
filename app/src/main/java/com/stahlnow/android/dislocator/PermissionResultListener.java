package com.stahlnow.android.dislocator;

public interface PermissionResultListener {
    public void onPermissionResult(int requestCode, String permissions[], int[] grantResults);
}