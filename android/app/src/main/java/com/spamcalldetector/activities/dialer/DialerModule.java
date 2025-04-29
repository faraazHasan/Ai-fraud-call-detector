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
        @SuppressLint("ServiceCast")
        TelecomManager telecomManager = (TelecomManager) reactContext.getSystemService(Context.TELECOM_SERVICE);
        Uri uri = Uri.fromParts("tel", phoneNumber, null);
        Bundle extras = new Bundle();
        extras.putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, false);

        if (ActivityCompat.checkSelfPermission(reactContext,
                Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {

            if (telecomManager.getDefaultDialerPackage().equals(reactContext.getPackageName())) {
                Log.d("Dialer", "Called from personal app");
                telecomManager.placeCall(uri, extras);
            } else {
                Intent callIntent = new Intent(Intent.ACTION_CALL, uri);
                callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                reactContext.startActivity(callIntent);
            }
        } else {
            Toast.makeText(reactContext, "Please allow permission", Toast.LENGTH_SHORT).show();
        }
    }
}