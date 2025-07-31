package com.spamcalldetector.activities.dialer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import android.util.Log;

public class DialerModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;

    public DialerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "DialerModule";
    }

    @ReactMethod
    public void dialNumber(String phoneNumber) {
        try {
            Log.d("Dialer", "Attempting to dial number: " + phoneNumber);
            
            // Check CALL_PHONE permission first
            if (ActivityCompat.checkSelfPermission(reactContext, Manifest.permission.CALL_PHONE) 
                != PackageManager.PERMISSION_GRANTED) {
                Log.e("Dialer", "CALL_PHONE permission not granted");
                Toast.makeText(reactContext, 
                    "Phone permission required. Please grant phone permissions in Settings.", 
                    Toast.LENGTH_LONG).show();
                return;
            }
            
            // Check READ_PHONE_STATE permission
            if (ActivityCompat.checkSelfPermission(reactContext, Manifest.permission.READ_PHONE_STATE) 
                != PackageManager.PERMISSION_GRANTED) {
                Log.e("Dialer", "READ_PHONE_STATE permission not granted");
                Toast.makeText(reactContext, 
                    "Phone state permission required. Please grant phone permissions in Settings.", 
                    Toast.LENGTH_LONG).show();
                return;
            }

            @SuppressLint("ServiceCast")
            TelecomManager telecomManager = (TelecomManager) reactContext.getSystemService(Context.TELECOM_SERVICE);
            
            if (telecomManager == null) {
                Log.e("Dialer", "TelecomManager not available");
                Toast.makeText(reactContext, "Calling service not available", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Uri uri = Uri.fromParts("tel", phoneNumber, null);
            Bundle extras = new Bundle();
            extras.putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, false);

            // Check if this app is the default dialer
            String defaultDialerPackage = telecomManager.getDefaultDialerPackage();
            String currentPackage = reactContext.getPackageName();
            
            if (currentPackage.equals(defaultDialerPackage)) {
                Log.d("Dialer", "Using TelecomManager.placeCall() as default dialer");
                telecomManager.placeCall(uri, extras);
            } else {
                Log.d("Dialer", "Using Intent.ACTION_CALL as non-default dialer");
                Intent callIntent = new Intent(Intent.ACTION_CALL, uri);
                callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                reactContext.startActivity(callIntent);
            }
            
            Log.d("Dialer", "Call initiated successfully");
            
        } catch (SecurityException e) {
            Log.e("Dialer", "Security exception when making call: " + e.getMessage());
            Toast.makeText(reactContext, 
                "Permission denied. Please grant phone permissions in Settings.", 
                Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e("Dialer", "Error making call: " + e.getMessage());
            Toast.makeText(reactContext, 
                "Failed to make call. Please try again or check your permissions.", 
                Toast.LENGTH_LONG).show();
        }
    }
}