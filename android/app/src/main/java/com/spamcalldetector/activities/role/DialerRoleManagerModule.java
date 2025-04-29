package com.spamcalldetector.activities.role;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;

public class DialerRoleManagerModule extends ReactContextBaseJavaModule {

    private static final int DEFAULT_DIALER_REQUEST_ID = 83;

    public DialerRoleManagerModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "DialerRoleManager";
    }

    @ReactMethod
    public void requestDialerRole(Callback callback) {
        Activity activity = getCurrentActivity(); // Get the current activity

        if (activity != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager roleManager = activity.getSystemService(RoleManager.class);

                if (roleManager != null) {
                    if (roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                        Log.d("DialerRole", "App is already the default dialer.");
                        callback.invoke("App is already the default dialer");
                    } else {
                        Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER);
                        if (intent != null) {
                            activity.startActivityForResult(intent, DEFAULT_DIALER_REQUEST_ID);
                            callback.invoke("Requesting dialer role");
                        } else {
                            callback.invoke("Failed to create intent for requesting dialer role");
                        }
                    }
                } else {
                    callback.invoke("RoleManager is not available");
                }
            } else {
                callback.invoke("Device does not support ROLE_DIALER (SDK version < Q)");
            }
        } else {
            callback.invoke("Activity is not available");
        }
    }
}
