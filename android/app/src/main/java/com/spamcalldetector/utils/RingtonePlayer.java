package com.spamcalldetector.utils;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

public class RingtonePlayer {
    private static volatile RingtonePlayer instance = null; // Ensuring single instance
    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private Vibrator vibrator;
    private AudioFocusRequest focusRequest;
    private boolean isPlaying = false;
    private boolean isVibrating = false;
    private final Context context;

    private RingtonePlayer(Context context) {
        this.context = context.getApplicationContext(); // Avoid memory leaks
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    // **Thread-safe Singleton**
    public static RingtonePlayer getInstance(Context context) {
        if (instance == null) {
            synchronized (RingtonePlayer.class) {
                if (instance == null) {
                    instance = new RingtonePlayer(context);
                }
            }
        }
        return instance;
    }

    public void playRingtone() {
        if (isPlaying || isVibrating) {
            Log.d("RingtonePlayer", "Ringtone or vibration already playing. Skipping...");
            return;
        }

        Log.d("RingtonePlayer", "Checking device ringer mode...");
        int ringerMode = audioManager.getRingerMode();

        if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            Log.d("RingtonePlayer", "Device in NORMAL mode. Playing ringtone...");
            requestAudioFocus();
            playSound();
        } else if (ringerMode == AudioManager.RINGER_MODE_VIBRATE || ringerMode == AudioManager.RINGER_MODE_SILENT) {
            Log.d("RingtonePlayer", "Device in SILENT/VIBRATE mode. Vibrating...");
            startVibration();
        }
    }

    private void requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build())
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(focusChange -> {
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                            Log.d("RingtonePlayer", "Lost Audio Focus! Stopping ringtone...");
                            stopRingtone();
                        }
                    })
                    .build();

            int focusResult = audioManager.requestAudioFocus(focusRequest);
            if (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.e("RingtonePlayer", "Failed to gain audio focus.");
            }
        } else {
            int focusResult = audioManager.requestAudioFocus(null,
                    AudioManager.STREAM_RING,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            if (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.e("RingtonePlayer", "Failed to gain audio focus.");
            }
        }
    }

    private void playSound() {
        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setDataSource(context, ringtoneUri);
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
            isPlaying = true;
            Log.d("RingtonePlayer", "Ringtone started.");
        } catch (Exception e) {
            Log.e("RingtonePlayer", "Error playing ringtone: " + e.getMessage());
        }
    }

    private void startVibration() {
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = { 0, 500, 500 }; // Vibrate 500ms, pause 500ms, repeat

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0)); // 0 means repeat indefinitely
            } else {
                vibrator.vibrate(pattern, 0);
            }
            isVibrating = true;
            Log.d("RingtonePlayer", "Vibration started.");
        }
    }

    public void stopRingtone() {
        if (mediaPlayer != null && isPlaying) {
            Log.d("RingtonePlayer", "Stopping ringtone...");
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            isPlaying = false;
        }

        if (vibrator != null && isVibrating) {
            vibrator.cancel();
            isVibrating = false;
            Log.d("RingtonePlayer", "Vibration stopped.");
        }

        if (focusRequest != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(focusRequest);
        } else {
            audioManager.abandonAudioFocus(null);
        }

        Log.d("RingtonePlayer", "Audio focus released.");
    }

    // **Properly release the instance**
    public static void releaseInstance() {
        if (instance != null) {
            instance.stopRingtone();
            instance = null;
        }
    }
}
