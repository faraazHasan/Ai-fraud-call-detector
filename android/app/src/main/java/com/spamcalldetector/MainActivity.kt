package com.spamcalldetector

import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate

import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import android.util.Log
import android.os.Environment
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.annotation.RequiresApi
import com.spamcalldetector.utils.CallStateManager
import com.spamcalldetector.activities.call.OutgoingCallActivity

class MainActivity : ReactActivity() {

  companion object {
        private const val DEFAULT_DIALER_REQUEST_ID = 83
        const val REQUEST_CODE_MANAGE_STORAGE = 101
        const val REQUEST_CODE_CAPTURE_AUDIO_OUTPUT = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        if (CallStateManager.isCallOngoing()) {
            val intent = Intent(this, OutgoingCallActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
        requestDefaultDialerRole()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == DEFAULT_DIALER_REQUEST_ID) {
            Log.d("Dialer", "RequestCode matched: $requestCode")

            if (resultCode != Activity.RESULT_OK) {
                Log.d("Dialer", "User did not set this app as the default dialer.")
                Toast.makeText(this, "Please set this app as default dialer app.", Toast.LENGTH_SHORT).show()
                // Redirect to settings for default apps configuration
                startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
            } else {
                Log.d("Dialer", "App set as the default dialer.")
            }
        }

        if (requestCode == REQUEST_CODE_MANAGE_STORAGE) {
            if (Environment.isExternalStorageManager()) {
                Log.d("Dialer", "MANAGE_EXTERNAL_STORAGE Permission granted!")
                // Proceed with file operations
            } else {
                Log.d("Dialer", "MANAGE_EXTERNAL_STORAGE Permission denied.")
                // Handle the permission denial (e.g., show a message or guide the user)
            }
        }
    }

    private fun requestDefaultDialerRole() {
        // Log the current SDK version
        Log.d("DialerRole", "SDK version: ${Build.VERSION.SDK_INT}")

        // Check if the SDK version is Q or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.d("DialerRole", "Device supports ROLE_DIALER")

            // Get the RoleManager system service
            val roleManager = getSystemService(RoleManager::class.java)

            // Log the RoleManager object to ensure it's being fetched correctly
            if (roleManager != null) {
                Log.d("DialerRole", "RoleManager obtained: $roleManager")
            } else {
                Log.e("DialerRole", "RoleManager not available.")
                return
            }

            // Check if the app is already the default dialer
            if (roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                Log.d("DialerRole", "App is already the default dialer.")
            } else {
                // Create the intent for requesting the dialer role
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)

                // Log the intent
                Log.d("DialerRole", "Intent created: $intent")

                if (intent != null) {
                    // Start the activity for result to request the default dialer role
                    startActivityForResult(intent, DEFAULT_DIALER_REQUEST_ID)
                    Log.d("DialerRole", "startActivityForResult called with request code: $DEFAULT_DIALER_REQUEST_ID")
                } else {
                    Log.e("DialerRole", "Failed to create intent for requesting dialer role.")
                }
            }
        } else {
            Log.d("DialerRole", "Device does not support ROLE_DIALER (SDK version < Q)")
        }
    }

  /**
   * Returns the name of the main component registered from JavaScript. This is used to schedule
   * rendering of the component.
   */
  override fun getMainComponentName(): String = "SpamCallDetector"

  /**
   * Returns the instance of the [ReactActivityDelegate]. We use [DefaultReactActivityDelegate]
   * which allows you to enable New Architecture with a single boolean flags [fabricEnabled]
   */
  override fun createReactActivityDelegate(): ReactActivityDelegate =
      DefaultReactActivityDelegate(this, mainComponentName, fabricEnabled)
}
