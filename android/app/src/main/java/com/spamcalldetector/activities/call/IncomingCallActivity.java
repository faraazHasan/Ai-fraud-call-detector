package com.spamcalldetector.activities.call;

import android.app.Activity;
import android.os.Bundle;
import com.facebook.react.ReactActivity;
import com.facebook.react.ReactActivityDelegate;
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint;
import com.facebook.react.defaults.DefaultReactActivityDelegate;

import java.lang.ref.WeakReference;

public class IncomingCallActivity extends ReactActivity {
    private static WeakReference<Activity> mCurrentActivity = new WeakReference<>(null);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCurrentActivity = new WeakReference<>(this);

        getWindow().addFlags(
            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );
    }

    public static Activity getActivity() {
        return mCurrentActivity.get();
    }

    @Override
    protected String getMainComponentName() {
        return "IncomingCall"; // This must match AppRegistry name in JS
    }

    @Override
    protected ReactActivityDelegate createReactActivityDelegate() {
        return new DefaultReactActivityDelegate(
            this,
            getMainComponentName(),
            DefaultNewArchitectureEntryPoint.getFabricEnabled()
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCurrentActivity.clear();
    }
}
