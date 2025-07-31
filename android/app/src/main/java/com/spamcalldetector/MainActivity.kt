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
import com.spamcalldetector.utils.PermissionManager
import com.facebook.react.modules.core.DeviceEventManagerModule

class MainActivity : ReactActivity() {

  companion object {
        private const val DEFAULT_DIALER_REQUEST_ID = 83
        const val REQUEST_CODE_MANAGE_STORAGE = 101
        const val REQUEST_CODE_CAPTURE_AUDIO_OUTPUT = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle calls to ensure app stays active during calls
        if (CallStateManager.isCallOngoing()) {
            // Keep screen on during active calls
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        
        // Don't request default dialer role immediately - let user open app first
        // requestDefaultDialerRole()
        
        // Handle navigation from notification
        handleNavigationIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            handleNavigationIntent(intent)
        }
    }
    
    private fun handleNavigationIntent(intent: Intent?) {
        Log.d("MainActivity", "handleNavigationIntent called with intent: $intent")
        if (intent != null) {
            val fromNotification = intent.getBooleanExtra("fromNotification", false)
            val navigateTo = intent.getStringExtra("navigateTo")
            val openMissedCalls = intent.getBooleanExtra("openMissedCalls", false)
            val phoneNumber = intent.getStringExtra("phoneNumber")
            
            Log.d("MainActivity", "Intent extras: fromNotification=$fromNotification, navigateTo=$navigateTo, openMissedCalls=$openMissedCalls, phoneNumber=$phoneNumber")
            
            if (fromNotification) {
                Log.d("MainActivity", "Processing notification intent for navigation")
                
                if (navigateTo == "RecentCalls") {
                // Try immediate navigation first
                sendNavigationEvent()
                
                // Also try with delays in case React context isn't ready
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    sendNavigationEvent()
                }, 500)
                
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    sendNavigationEvent()
                }, 1000)
                
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    sendNavigationEvent()
                }, 2000)
                }
            }
        } else {
            Log.d("MainActivity", "No navigation intent detected")
        }
    }
    
    private fun sendNavigationEvent() {
        try {
            val reactInstanceManager = reactNativeHost.reactInstanceManager
            val reactContext = reactInstanceManager.currentReactContext
            if (reactContext != null) {
                // Try DeviceEventEmitter first
                try {
                    reactContext
                        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                        .emit("NAVIGATE_TO_RECENT_CALLS", null)
                    Log.d("MainActivity", "Successfully sent navigation event via DeviceEventEmitter")
                } catch (e: Exception) {
                    Log.w("MainActivity", "DeviceEventEmitter failed, trying alternative method: ${e.message}")
                }
                
                // Log that we attempted navigation
                Log.d("MainActivity", "Navigation event sent via DeviceEventEmitter")
            } else {
                Log.w("MainActivity", "React context not available for navigation")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error sending navigation event: ${e.message}")
        }
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            PermissionManager.REQUEST_CODE_PHONE_PERMISSIONS -> {
                Log.d("MainActivity", "Phone permissions result: ${grantResults.contentToString()}")
            }
            PermissionManager.REQUEST_CODE_CONTACTS_PERMISSIONS -> {
                Log.d("MainActivity", "Contacts permissions result: ${grantResults.contentToString()}")
            }
            PermissionManager.REQUEST_CODE_STORAGE_PERMISSIONS -> {
                Log.d("MainActivity", "Storage permissions result: ${grantResults.contentToString()}")
            }
            PermissionManager.REQUEST_CODE_AUDIO_PERMISSIONS -> {
                Log.d("MainActivity", "Audio permissions result: ${grantResults.contentToString()}")
            }
            PermissionManager.REQUEST_CODE_ALL_PERMISSIONS -> {
                Log.d("MainActivity", "All permissions result: ${grantResults.contentToString()}")
            }
        }
        
        // Send permission result to React Native
        try {
            val reactInstanceManager = reactNativeHost.reactInstanceManager
            val reactContext = reactInstanceManager.currentReactContext
            if (reactContext != null) {
                reactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                    .emit("PERMISSION_RESULT", null)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error sending permission result event: ${e.message}")
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

    override fun onBackPressed() {
        if (CallStateManager.isCallOngoing()) {
            // If a call is active, don't allow the app to close
            val toast = Toast.makeText(
                this,
                "You cannot exit the app while a call is in progress",
                Toast.LENGTH_SHORT
            )
            toast.show()
            
            // Redirect to the call activity if there's an active call
            val intent = Intent(this, OutgoingCallActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        } else {
            super.onBackPressed()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (CallStateManager.isCallOngoing()) {
            Log.d("MainActivity", "User trying to leave app during active call")
            // Optional: you can add code here to bring the app back to foreground
            // or create a notification to make it easy to return
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
