package com.example.przestrzeliny_app;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public class PermissionHelper {
    public static final int REQUEST_CODE_PERMISSIONS = 10;
    public static final String[] REQUIRED_PERMISSIONS = new String[]{ Manifest.permission.CAMERA };
    private final Activity activity;
    private final PermissionListener listener;
    public interface PermissionListener {
        void onPermissionsGranted(); // Sygnał: Zezwolono

        void onPermissionsDenied();  // Sygnał: Odmówiono
    }
        public PermissionHelper(Activity activity, PermissionListener listener) {
            this.activity = activity;
            this.listener = listener;

    }
    public boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    public void requestPermissions() {
        activity.requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
    }
    public void handlePermissionsResult(int requestCode, int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                listener.onPermissionsGranted(); // zezwolono
            } else {
                listener.onPermissionsDenied();  // odmówiono
            }
        }
    }
}
