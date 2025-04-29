package com.spamcalldetector.utils;

import android.app.ActivityManager;
import android.content.Context;

import java.util.List;

public class ActivityStates {

    public static boolean isMainActivityRunning(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null)
            return false;

        List<ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
        if (processes == null)
            return false;

        for (ActivityManager.RunningAppProcessInfo process : processes) {
            if (process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    process.processName.equals(context.getPackageName())) {
                return true;
            }
        }

        return false;
    }
}
