package com.spamcalldetector

import android.app.Application
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactPackage
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.load
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.facebook.react.defaults.DefaultReactNativeHost
import com.facebook.react.soloader.OpenSourceMergedSoMapping
import com.facebook.soloader.SoLoader
import android.content.res.Configuration
import com.facebook.react.bridge.ReactApplicationContext;

import com.spamcalldetector.activities.call.CallActivityPackage
import com.spamcalldetector.activities.call.CallHistoryPackage
import com.spamcalldetector.activities.call.MissedCallPackage
import com.spamcalldetector.activities.role.DialerRoleManagerPackage
import com.spamcalldetector.activities.dialer.DialerPackage
import com.spamcalldetector.activities.permission.ManageExternalStoragePackage
import com.spamcalldetector.activities.contacts.ContactsPackage
import com.spamcalldetector.utils.PermissionManagerPackage
import com.zmxv.RNSound.RNSoundPackage;

class MainApplication : Application(), ReactApplication {

  override val reactNativeHost: ReactNativeHost =
      object : DefaultReactNativeHost(this) {
        override fun getPackages(): List<ReactPackage> =
            PackageList(this).packages.apply {
              // Packages that cannot be autolinked yet can be added manually here, for example:
              add(DialerRoleManagerPackage()).also { android.util.Log.d("MainApplication", "Added DialerRoleManagerPackage") };
              add(DialerPackage()).also { android.util.Log.d("MainApplication", "Added DialerPackage") };
              add(CallActivityPackage()).also { android.util.Log.d("MainApplication", "Added CallActivityPackage") };
              add(ManageExternalStoragePackage()).also { android.util.Log.d("MainApplication", "Added ManageExternalStoragePackage") };
              add(ContactsPackage()).also { android.util.Log.d("MainApplication", "Added ContactsPackage") };
              add(RNSoundPackage()).also { android.util.Log.d("MainApplication", "Added RNSoundPackage") };
              add(CallHistoryPackage()).also { android.util.Log.d("MainApplication", "Added CallHistoryPackage") };
              add(MissedCallPackage()).also { android.util.Log.d("MainApplication", "Added MissedCallPackage") };
              add(PermissionManagerPackage()).also { android.util.Log.d("MainApplication", "Added PermissionManagerPackage") };
            }

        override fun getJSMainModuleName(): String = "index"

        override fun getUseDeveloperSupport(): Boolean = BuildConfig.DEBUG

        override val isNewArchEnabled: Boolean = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
        override val isHermesEnabled: Boolean = BuildConfig.IS_HERMES_ENABLED
      }

  override val reactHost: ReactHost
    get() = getDefaultReactHost(applicationContext, reactNativeHost)

  override fun onCreate() {
    super.onCreate()
    SoLoader.init(this, OpenSourceMergedSoMapping)
    if (BuildConfig.IS_NEW_ARCHITECTURE_ENABLED) {
      // If you opted-in for the New Architecture, we load the native entry point for this app.
      load()
    }
  }
}