package com.spamcalldetector.activities.permission;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.os.Environment;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;

public class ManageExternalStorageModule extends ReactContextBaseJavaModule {

    public ManageExternalStorageModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "ManageExternalStorage";
    }

    // Method to request MANAGE_EXTERNAL_STORAGE permission
    @ReactMethod
    public void requestManageExternalStoragePermission(Callback successCallback, Callback errorCallback) {
        Log.d("Dialer", "Asking for ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION");

        try {
            // Check Android version (only available on Android 11 and above)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    // Permission is not granted, open settings to request permission
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    Activity currentActivity = getCurrentActivity();

                    if (currentActivity != null) {
                        currentActivity.startActivityForResult(intent, 1001); // Request permission
                        Log.d("Dialer", "Permission request sent to settings");
                        successCallback.invoke("Permission request sent to settings");
                    } else {
                        errorCallback.invoke("Current activity is null");
                    }
                } else {
                    Log.d("Dialer", "Permission already granted");
                    successCallback.invoke("Permission already granted");
                }
            } else {
                // For devices below Android 11, permission is not needed
                Log.d("Dialer", "Permission not required for versions below Android 11");
                successCallback.invoke("Permission not required for versions below Android 11");
            }
        } catch (Exception e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    // Override onActivityResult to handle the result of the permission request
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (Environment.isExternalStorageManager()) {
                Log.d("Dialer", "Permission granted");
            } else {
                Log.d("Dialer", "Permission denied");
            }
        }
    }
}
