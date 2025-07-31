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
import com.facebook.react.bridge.Promise;

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
    public void checkDialerRole(Promise promise) {
        try {
            Activity activity = getCurrentActivity();
            
            if (activity != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    RoleManager roleManager = activity.getSystemService(RoleManager.class);
                    
                    if (roleManager != null) {
                        boolean isDefaultDialer = roleManager.isRoleHeld(RoleManager.ROLE_DIALER);
                        promise.resolve(isDefaultDialer);
                    } else {
                        promise.resolve(false);
                    }
                } else {
                    // For older versions, assume false since ROLE_DIALER is not supported
                    promise.resolve(false);
                }
            } else {
                promise.resolve(false);
            }
        } catch (Exception e) {
            Log.e("DialerRole", "Error checking dialer role: " + e.getMessage());
            promise.resolve(false);
        }
    }

    @ReactMethod
    public void requestDialerRole(Promise promise) {
        try {
            Activity activity = getCurrentActivity();

            if (activity != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    RoleManager roleManager = activity.getSystemService(RoleManager.class);

                    if (roleManager != null) {
                        if (roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                            Log.d("DialerRole", "App is already the default dialer.");
                            promise.resolve(true);
                        } else {
                            Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER);
                            if (intent != null) {
                                activity.startActivityForResult(intent, DEFAULT_DIALER_REQUEST_ID);
                                // Note: The actual result will be handled in MainActivity's onActivityResult
                                // For now, we resolve with true to indicate the request was initiated
                                promise.resolve(true);
                            } else {
                                Log.e("DialerRole", "Failed to create intent for requesting dialer role");
                                promise.resolve(false);
                            }
                        }
                    } else {
                        Log.e("DialerRole", "RoleManager is not available");
                        promise.resolve(false);
                    }
                } else {
                    Log.e("DialerRole", "Device does not support ROLE_DIALER (SDK version < Q)");
                    promise.resolve(false);
                }
            } else {
                Log.e("DialerRole", "Activity is not available");
                promise.resolve(false);
            }
        } catch (Exception e) {
            Log.e("DialerRole", "Error requesting dialer role: " + e.getMessage());
            promise.resolve(false);
        }
    }
}
