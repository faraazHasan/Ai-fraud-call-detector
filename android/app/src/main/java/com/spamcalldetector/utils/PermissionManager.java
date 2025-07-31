package com.spamcalldetector.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;
import java.util.ArrayList;
import java.util.List;

public class PermissionManager extends ReactContextBaseJavaModule {
    
    private static final String TAG = "PermissionManager";
    
    // Permission request codes
    public static final int REQUEST_CODE_PHONE_PERMISSIONS = 1001;
    public static final int REQUEST_CODE_CONTACTS_PERMISSIONS = 1002;
    public static final int REQUEST_CODE_STORAGE_PERMISSIONS = 1003;
    public static final int REQUEST_CODE_AUDIO_PERMISSIONS = 1004;
    public static final int REQUEST_CODE_NOTIFICATION_PERMISSIONS = 1005;
    public static final int REQUEST_CODE_ALL_PERMISSIONS = 1006;
    
    // Required permissions grouped by functionality
    public static final String[] PHONE_PERMISSIONS = {
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.MANAGE_OWN_CALLS,
        Manifest.permission.READ_CALL_LOG
    };
    
    public static final String[] CONTACTS_PERMISSIONS = {
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS
    };
    
    public static final String[] STORAGE_PERMISSIONS = {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    
    public static final String[] AUDIO_PERMISSIONS = {
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.MODIFY_AUDIO_SETTINGS
    };
    
    public static final String[] NOTIFICATION_PERMISSIONS = 
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? 
        new String[]{Manifest.permission.POST_NOTIFICATIONS} : 
        new String[]{};
    
    // All critical permissions
    public static final String[] ALL_PERMISSIONS = {
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.MANAGE_OWN_CALLS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.MODIFY_AUDIO_SETTINGS,
        Manifest.permission.SYSTEM_ALERT_WINDOW,
        Manifest.permission.VIBRATE,
        Manifest.permission.WAKE_LOCK
    };
    
    public PermissionManager(ReactApplicationContext reactContext) {
        super(reactContext);
    }
    
    @Override
    public String getName() {
        return "PermissionManager";
    }
    
    /**
     * Check if a specific permission is granted
     */
    public static boolean hasPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Check if all permissions in an array are granted
     */
    public static boolean hasAllPermissions(Context context, String[] permissions) {
        for (String permission : permissions) {
            if (!hasPermission(context, permission)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Get list of missing permissions from an array
     */
    public static List<String> getMissingPermissions(Context context, String[] permissions) {
        List<String> missingPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (!hasPermission(context, permission)) {
                missingPermissions.add(permission);
            }
        }
        return missingPermissions;
    }
    
    /**
     * Request permissions from activity
     */
    public static void requestPermissions(Activity activity, String[] permissions, int requestCode) {
        List<String> missingPermissions = getMissingPermissions(activity, permissions);
        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                activity, 
                missingPermissions.toArray(new String[0]), 
                requestCode
            );
        }
    }
    
    /**
     * Check if MANAGE_EXTERNAL_STORAGE permission is granted (Android 11+)
     */
    public static boolean hasManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return true; // Not required on older versions
    }
    
    /**
     * Check if SYSTEM_ALERT_WINDOW permission is granted
     */
    public static boolean hasSystemAlertWindowPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true; // Not required on older versions
    }
    
    // React Native methods
    
    @ReactMethod
    public void checkAllPermissions(Promise promise) {
        try {
            WritableMap result = Arguments.createMap();
            Context context = getReactApplicationContext();
            
            // Check phone permissions
            result.putBoolean("hasPhonePermissions", hasAllPermissions(context, PHONE_PERMISSIONS));
            
            // Check contacts permissions
            result.putBoolean("hasContactsPermissions", hasAllPermissions(context, CONTACTS_PERMISSIONS));
            
            // Check storage permissions
            result.putBoolean("hasStoragePermissions", hasAllPermissions(context, STORAGE_PERMISSIONS));
            
            // Check audio permissions
            result.putBoolean("hasAudioPermissions", hasAllPermissions(context, AUDIO_PERMISSIONS));
            
            // Check notification permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.putBoolean("hasNotificationPermissions", hasAllPermissions(context, NOTIFICATION_PERMISSIONS));
            } else {
                result.putBoolean("hasNotificationPermissions", true);
            }
            
            // Check special permissions
            result.putBoolean("hasManageExternalStorage", hasManageExternalStoragePermission());
            result.putBoolean("hasSystemAlertWindow", hasSystemAlertWindowPermission(context));
            
            // Check if all critical permissions are granted
            result.putBoolean("hasAllCriticalPermissions", 
                hasAllPermissions(context, ALL_PERMISSIONS) && 
                hasManageExternalStoragePermission() && 
                hasSystemAlertWindowPermission(context)
            );
            
            promise.resolve(result);
        } catch (Exception e) {
            Log.e(TAG, "Error checking permissions: " + e.getMessage());
            promise.reject("PERMISSION_CHECK_ERROR", e.getMessage());
        }
    }
    
    @ReactMethod
    public void checkPhonePermissions(Promise promise) {
        try {
            Context context = getReactApplicationContext();
            boolean hasPermissions = hasAllPermissions(context, PHONE_PERMISSIONS);
            promise.resolve(hasPermissions);
        } catch (Exception e) {
            Log.e(TAG, "Error checking phone permissions: " + e.getMessage());
            promise.reject("PERMISSION_CHECK_ERROR", e.getMessage());
        }
    }
    
    @ReactMethod
    public void checkContactsPermissions(Promise promise) {
        try {
            Context context = getReactApplicationContext();
            boolean hasPermissions = hasAllPermissions(context, CONTACTS_PERMISSIONS);
            promise.resolve(hasPermissions);
        } catch (Exception e) {
            Log.e(TAG, "Error checking contacts permissions: " + e.getMessage());
            promise.reject("PERMISSION_CHECK_ERROR", e.getMessage());
        }
    }
    
    @ReactMethod
    public void checkStoragePermissions(Promise promise) {
        try {
            Context context = getReactApplicationContext();
            boolean hasPermissions = hasAllPermissions(context, STORAGE_PERMISSIONS) && hasManageExternalStoragePermission();
            promise.resolve(hasPermissions);
        } catch (Exception e) {
            Log.e(TAG, "Error checking storage permissions: " + e.getMessage());
            promise.reject("PERMISSION_CHECK_ERROR", e.getMessage());
        }
    }
    
    @ReactMethod
    public void requestAllPermissions(Promise promise) {
        try {
            Activity activity = getCurrentActivity();
            if (activity != null) {
                requestPermissions(activity, ALL_PERMISSIONS, REQUEST_CODE_ALL_PERMISSIONS);
                promise.resolve("Permission request initiated");
            } else {
                promise.reject("NO_ACTIVITY", "Activity not available");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error requesting permissions: " + e.getMessage());
            promise.reject("PERMISSION_REQUEST_ERROR", e.getMessage());
        }
    }
    
    @ReactMethod
    public void requestPhonePermissions(Promise promise) {
        try {
            Activity activity = getCurrentActivity();
            if (activity != null) {
                requestPermissions(activity, PHONE_PERMISSIONS, REQUEST_CODE_PHONE_PERMISSIONS);
                promise.resolve("Phone permission request initiated");
            } else {
                promise.reject("NO_ACTIVITY", "Activity not available");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error requesting phone permissions: " + e.getMessage());
            promise.reject("PERMISSION_REQUEST_ERROR", e.getMessage());
        }
    }
    
    @ReactMethod
    public void requestContactsPermissions(Promise promise) {
        try {
            Activity activity = getCurrentActivity();
            if (activity != null) {
                requestPermissions(activity, CONTACTS_PERMISSIONS, REQUEST_CODE_CONTACTS_PERMISSIONS);
                promise.resolve("Contacts permission request initiated");
            } else {
                promise.reject("NO_ACTIVITY", "Activity not available");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error requesting contacts permissions: " + e.getMessage());
            promise.reject("PERMISSION_REQUEST_ERROR", e.getMessage());
        }
    }
}
