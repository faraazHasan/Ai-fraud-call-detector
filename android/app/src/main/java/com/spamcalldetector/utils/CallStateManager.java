package com.spamcalldetector.utils;

public class CallStateManager {
    private static boolean isCallOngoing = false;

    public static void setCallOngoing(boolean ongoing) {
        isCallOngoing = ongoing;
    }

    public static boolean isCallOngoing() {
        return isCallOngoing;
    }
}
