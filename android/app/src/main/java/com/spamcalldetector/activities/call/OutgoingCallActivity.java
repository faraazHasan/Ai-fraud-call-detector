package com.spamcalldetector.activities.call;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import com.facebook.react.ReactActivity;
import com.facebook.react.ReactActivityDelegate;
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint;
import com.facebook.react.defaults.DefaultReactActivityDelegate;
import com.spamcalldetector.utils.CallStateManager;
import com.spamcalldetector.utils.ActivityStates;
import com.spamcalldetector.MainActivity;

import java.lang.ref.WeakReference;

public class OutgoingCallActivity extends ReactActivity {
    private static WeakReference<Activity> mCurrentActivity = new WeakReference<>(null);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCurrentActivity = new WeakReference<>(this);

        // Prevent screen from sleeping and show over lock screen
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTaskDescription(new ActivityManager.TaskDescription(null, null, 0));
        }

        setFinishOnTouchOutside(false); // Just in case user tries to dismiss like a dialog
    }

    @Override
    public void onBackPressed() {
        // Disable back button to prevent manual close
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCurrentActivity.clear();

        // Relaunch the screen if the call is still ongoing
        if (CallStateManager.isCallOngoing()) {
            Intent intent = new Intent(this, OutgoingCallActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }
    }

    public static Activity getActivity() {
        return mCurrentActivity.get();
    }

    @Override
    protected String getMainComponentName() {
        return "OutgoingCall";
    }

    @Override
    protected ReactActivityDelegate createReactActivityDelegate() {
        return new DefaultReactActivityDelegate(this, getMainComponentName(),
                DefaultNewArchitectureEntryPoint.getFabricEnabled());
    }
}
