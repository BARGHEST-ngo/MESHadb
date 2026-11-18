// Copyright (c) Tailscale Inc & contributors
// SPDX-License-Identifier: BSD-3-Clause
//
// Portions Copyright (c) BARGHEST
// SPDX-License-Identifier: AGPL-3.0-or-later
//
// This file contains code originally from Tailscale (BSD-3-Clause)
// with modifications by BARGHEST. The modified version is licensed
// under AGPL-3.0-or-later. See LICENSE for details.

package com.barghest.mesh

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.RestrictionsManager
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.barghest.mesh.mdm.MDMSettings
import com.barghest.mesh.mdm.MDMSettingsChangedReceiver
import com.barghest.mesh.ui.model.Ipn
import com.barghest.mesh.ui.notifier.Notifier
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.barghest.mesh.ui.viewModel.AppViewModel
import com.barghest.mesh.ui.viewModel.AppViewModelFactory
import com.barghest.mesh.util.TSLog
import java.io.IOException
import java.security.GeneralSecurityException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class App : UninitializedApp(), ViewModelStoreOwner {
  val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  companion object {
    // Channels from the Tailscale-based versions, removed on start.
    private val LEGACY_CHANNEL_IDS = listOf("barghest-files", "barghest-health")
    private const val TAG = "App"
    private lateinit var appInstance: App
    /**
     * Initializes the app (if necessary) and returns the singleton app instance. Always use this
     * function to obtain an App reference to make sure the app initializes.
     */
    @JvmStatic
    fun get(): App {
      appInstance.initOnce()
      return appInstance
    }
  }

  private lateinit var mdmChangeReceiver: MDMSettingsChangedReceiver
  override val viewModelStore: ViewModelStore
    get() = appViewModelStore

  private val appViewModelStore: ViewModelStore by lazy { ViewModelStore() }

  override fun onCreate() {
    super.onCreate()
    appInstance = this
    setUnprotectedInstance(this)
    mdmChangeReceiver = MDMSettingsChangedReceiver()
    val filter = IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED)
    registerReceiver(mdmChangeReceiver, filter)
    createNotificationChannel(
        STATUS_CHANNEL_ID,
        getString(R.string.vpn_status),
        getString(R.string.optional_notifications_which_display_the_status_of_the_vpn_tunnel),
        NotificationManagerCompat.IMPORTANCE_MIN)
    LEGACY_CHANNEL_IDS.forEach { notificationManager.deleteNotificationChannel(it) }
    createNotificationChannel(
        CHANNEL_ID,
        "Security Alerts",
        "Notifications for security issues such as developer options and outdated security patches",
        NotificationManagerCompat.IMPORTANCE_DEFAULT)
    val securityWorkRequest = PeriodicWorkRequestBuilder<SecWorker>(15, TimeUnit.MINUTES).build()
    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
        "security_check",
        ExistingPeriodicWorkPolicy.KEEP,
        securityWorkRequest)
  }

  override fun onTerminate() {
    super.onTerminate()
    notificationManager.cancelAll()
    applicationScope.cancel()
    viewModelStore.clear()
    unregisterReceiver(mdmChangeReceiver)
  }

  @Volatile private var isInitialized = false

  @Synchronized
  private fun initOnce() {
    if (isInitialized) {
      return
    }
    initializeApp()
    isInitialized = true
  }

  private fun initializeApp() {
    initViewModels()
    applicationScope.launch {
      val rm = getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
      MDMSettings.update(get(), rm)
      combine(Notifier.state, MDMSettings.forceEnabled.flow) { state, forceEnabled ->
            state to forceEnabled
          }
          .distinctUntilChanged()
          .collect { (state, hideDisconnectAction) ->
            val ableToStartVPN = state > Ipn.State.NeedsMachineAuth
            // If VPN is stopped, show a disconnected notification. If it is running as a
            // foreground service, IPNService will show a connected notification.
            if (state == Ipn.State.Stopped) {
              notifyStatus(vpnRunning = false, hideDisconnectAction = hideDisconnectAction.value)
            }
            val vpnRunning = state == Ipn.State.Starting || state == Ipn.State.Running
            updateConnStatus(ableToStartVPN)
            QuickToggleService.setVPNRunning(vpnRunning)
            // Update notification status when VPN is running
            if (vpnRunning) {
              notifyStatus(vpnRunning = true, hideDisconnectAction = hideDisconnectAction.value)
            }
          }
    }
    TSLog.init(this)
  }

  private fun initViewModels() {
    appViewModel = ViewModelProvider(this, AppViewModelFactory(this)).get(AppViewModel::class.java)
  }

  @Throws(IOException::class, GeneralSecurityException::class)
  fun getEncryptedPrefs(): SharedPreferences {
    val key = MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    return EncryptedSharedPreferences.create(
        this,
        "secret_shared_prefs",
        key,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
  }

  /*
   * setAbleToStartVPN remembers whether or not we're able to start the VPN
   * by storing this in a shared preference. This allows us to check this
   * value without needing a fully initialized instance of the application.
   */
  private fun updateConnStatus(ableToStartVPN: Boolean) {
    setAbleToStartVPN(ableToStartVPN)
    QuickToggleService.updateTile()
    TSLog.d("App", "Set Tile Ready: $ableToStartVPN")
  }
}
/**
 * UninitializedApp contains all of the methods of App that can be used without having to initialize
 * the Go backend. This is useful when you want to access functions on the App without creating side
 * effects from starting the Go backend (such as launching the VPN).
 */
open class UninitializedApp : Application() {
  companion object {
    const val TAG = "UninitializedApp"
    const val STATUS_NOTIFICATION_ID = 1
    const val STATUS_CHANNEL_ID = "barghest-status"
    const val CHANNEL_ID = "barghest-security"
    // Key for shared preference that tracks whether or not we're able to start
    // the VPN (i.e. we're logged in and machine is authorized).
    private const val ABLE_TO_START_VPN_KEY = "ableToStartVPN"
    private const val DISALLOWED_APPS_KEY = "disallowedApps"
    // File for shared preferences that are not encrypted.
    private const val UNENCRYPTED_PREFERENCES = "unencrypted"
    private lateinit var appInstance: UninitializedApp
    lateinit var notificationManager: NotificationManagerCompat

    lateinit var appViewModel: AppViewModel

    @JvmStatic
    fun get(): UninitializedApp {
      return appInstance
    }
  }

  protected fun setUnprotectedInstance(instance: UninitializedApp) {
    appInstance = instance
  }

  protected fun setAbleToStartVPN(rdy: Boolean) {
    getUnencryptedPrefs().edit().putBoolean(ABLE_TO_START_VPN_KEY, rdy).apply()
  }
  /** This function can be called without initializing the App. */
  fun isAbleToStartVPN(): Boolean {
    return getUnencryptedPrefs().getBoolean(ABLE_TO_START_VPN_KEY, false)
  }

  private fun getUnencryptedPrefs(): SharedPreferences {
    return getSharedPreferences(UNENCRYPTED_PREFERENCES, MODE_PRIVATE)
  }

  fun startVPN() {
    val intent = Intent(this, IPNService::class.java).apply { action = IPNService.ACTION_START_VPN }
    // FLAG_UPDATE_CURRENT ensures that if the intent is already pending, the existing intent will
    // be updated rather than creating multiple redundant instances.
    val pendingIntent =
        PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE // FLAG_IMMUTABLE for Android 12+
            )
    try {
      pendingIntent.send()
    } catch (foregroundServiceStartException: IllegalStateException) {
      TSLog.e(
          TAG,
          "startVPN hit ForegroundServiceStartNotAllowedException: $foregroundServiceStartException")
    } catch (securityException: SecurityException) {
      TSLog.e(TAG, "startVPN hit SecurityException: $securityException")
    } catch (e: Exception) {
      TSLog.e(TAG, "startVPN hit exception: $e")
    }
  }

  fun stopVPN() {
    val intent = Intent(this, IPNService::class.java).apply { action = IPNService.ACTION_STOP_VPN }
    try {
      startService(intent)
    } catch (illegalStateException: IllegalStateException) {
      TSLog.e(TAG, "stopVPN hit IllegalStateException in startService(): $illegalStateException")
    } catch (e: Exception) {
      TSLog.e(TAG, "stopVPN hit exception in startService(): $e")
    }
  }

  fun restartVPN() {
    val intent =
        Intent(this, IPNService::class.java).apply { action = IPNService.ACTION_RESTART_VPN }
    try {
      startService(intent)
    } catch (illegalStateException: IllegalStateException) {
      TSLog.e(TAG, "restartVPN hit IllegalStateException in startService(): $illegalStateException")
    } catch (e: Exception) {
      TSLog.e(TAG, "restartVPN hit exception in startService(): $e")
    }
  }

  fun createNotificationChannel(id: String, name: String, description: String, importance: Int) {
    val channel = NotificationChannel(id, name, importance)
    channel.description = description
    notificationManager = NotificationManagerCompat.from(this)
    notificationManager.createNotificationChannel(channel)
  }

  fun notifyStatus(vpnRunning: Boolean, hideDisconnectAction: Boolean) {
    notifyStatus(buildStatusNotification(vpnRunning, hideDisconnectAction))
  }

  fun notifyStatus(notification: Notification) {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED) {
      // TODO: Consider calling
      //    ActivityCompat#requestPermissions
      // here to request the missing permissions, and then overriding
      //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
      //                                          int[] grantResults)
      // to handle the case where the user grants the permission. See the documentation
      // for ActivityCompat#requestPermissions for more details.
      return
    }
    notificationManager.notify(STATUS_NOTIFICATION_ID, notification)
  }

  fun buildStatusNotification(vpnRunning: Boolean, hideDisconnectAction: Boolean): Notification {
    val title = getString(if (vpnRunning) R.string.connected else R.string.not_connected)
    val icon = if (vpnRunning) R.drawable.ic_notification else R.drawable.ic_notification_disabled
    val action =
        if (vpnRunning) IPNReceiver.INTENT_DISCONNECT_VPN else IPNReceiver.INTENT_CONNECT_VPN
    val actionLabel = getString(if (vpnRunning) R.string.disconnect else R.string.connect)
    val buttonIntent = Intent(this, IPNReceiver::class.java).apply { this.action = action }
    val pendingButtonIntent: PendingIntent =
        PendingIntent.getBroadcast(
            this,
            0,
            buttonIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val intent =
        Intent(this, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    val pendingIntent: PendingIntent =
        PendingIntent.getActivity(
            this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val builder =
        NotificationCompat.Builder(this, STATUS_CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setAutoCancel(!vpnRunning)
            .setOnlyAlertOnce(!vpnRunning)
            .setOngoing(vpnRunning)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
    if (!vpnRunning || !hideDisconnectAction) {
      builder.addAction(
          NotificationCompat.Action.Builder(0, actionLabel, pendingButtonIntent).build())
    }
    return builder.build()
  }

  fun updateUserDisallowedPackageNames(packageNames: List<String>) {
    if (packageNames.any { it.isEmpty() }) {
      TSLog.e(TAG, "updateUserDisallowedPackageNames called with empty packageName(s)")
      return
    }
    getUnencryptedPrefs().edit().putStringSet(DISALLOWED_APPS_KEY, packageNames.toSet()).apply()
    this.restartVPN()
  }

  fun disallowedPackageNames(): List<String> {
    val mdmDisallowed =
        MDMSettings.excludedPackages.flow.value.value?.split(",")?.map { it.trim() } ?: emptyList()
    if (mdmDisallowed.isNotEmpty()) {
      TSLog.d(TAG, "Excluded application packages were set via MDM: $mdmDisallowed")
      return builtInDisallowedPackageNames + mdmDisallowed
    }
    val userDisallowed =
        getUnencryptedPrefs().getStringSet(DISALLOWED_APPS_KEY, emptySet())?.toList() ?: emptyList()
    return builtInDisallowedPackageNames + userDisallowed
  }

  fun getAppScopedViewModel(): AppViewModel {
    return appViewModel
  }

  val builtInDisallowedPackageNames: List<String> =
      listOf(
          // RCS/Jibe https://github.com/tailscale/tailscale/issues/2322
          "com.google.android.apps.messaging",
          // Android Auto https://github.com/tailscale/tailscale/issues/3828
          "com.google.android.projection.gearhead",
          // GoPro https://github.com/tailscale/tailscale/issues/2554
          "com.gopro.smarty",
          // Sonos https://github.com/tailscale/tailscale/issues/2548
          "com.sonos.acr",
          "com.sonos.acr2",
          // Google Chromecast https://github.com/tailscale/tailscale/issues/3636
          "com.google.android.apps.chromecast.app",
          // Voicemail https://github.com/tailscale/tailscale/issues/13199
          "com.samsung.attvvm",
          "com.att.mobile.android.vvm",
          "com.tmobile.vvm.application",
          "com.metropcs.service.vvm",
          "com.mizmowireless.vvm",
          "com.vna.service.vvm",
          "com.dish.vvm",
          "com.comcast.modesto.vvm.client",
          // Android Connectivity Service https://github.com/tailscale/tailscale/issues/14128
          "com.google.android.apps.scone",
      )
}
