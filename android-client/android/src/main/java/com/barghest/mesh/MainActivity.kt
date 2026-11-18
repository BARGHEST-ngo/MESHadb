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

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.RestrictionsManager
import android.content.pm.ActivityInfo
import android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE
import android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.barghest.mesh.mdm.MDMSettings
import com.barghest.mesh.mdm.ShowHide
import com.barghest.mesh.ui.theme.AppTheme
import com.barghest.mesh.ui.util.universalFit
import com.barghest.mesh.ui.model.Ipn
import com.barghest.mesh.ui.view.ADBSetup
import com.barghest.mesh.ui.view.mesh.MeshFlow
import com.barghest.mesh.ui.view.mesh.MeshLiveEnv
import com.barghest.mesh.ui.view.mesh.MeshOnboarding
import com.barghest.mesh.ui.view.AboutView
import com.barghest.mesh.ui.view.DeviceInfoView
import com.barghest.mesh.ui.view.MDMSettingsDebugView
import com.barghest.mesh.ui.view.NotificationsView
import com.barghest.mesh.ui.view.SplitTunnelAppPickerView
import com.barghest.mesh.ui.view.onboarding.QRScanStep
import com.barghest.mesh.ui.viewModel.AppViewModel
import com.barghest.mesh.ui.viewModel.MainViewModel
import com.barghest.mesh.ui.viewModel.MainViewModelFactory
import com.barghest.mesh.util.TSLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {
    private lateinit var navController: NavHostController
    private lateinit var vpnPermissionLauncher: ActivityResultLauncher<Intent>
    private lateinit var appViewModel: AppViewModel
    private lateinit var viewModel: MainViewModel

    companion object {
        private const val TAG = "Main Activity"
        private const val START_AT_ROOT = "startAtRoot"
    }

    private var showPinDialog by mutableStateOf(false)
    private var pinDialogCallback: ((String) -> Unit)? = null
    private var showQRScanFromHome by mutableStateOf(false)
    private var adbWifiEnabled by mutableStateOf(false)

    private fun Context.isLandscapeCapable(): Boolean =
        (resources.configuration.screenLayout and SCREENLAYOUT_SIZE_MASK) >=
            SCREENLAYOUT_SIZE_LARGE

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
	
        // grab app to make sure it initializes
        App.get()
        appViewModel = (application as App).getAppScopedViewModel()
        viewModel =
            ViewModelProvider(
                this,
                MainViewModelFactory(appViewModel),
            ).get(MainViewModel::class.java)

        val rm = getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
        MDMSettings.update(App.get(), rm)
        if (MDMSettings.onboardingFlow.flow.value.value == ShowHide.Hide ||
            MDMSettings.authKey.flow.value.value != null
        ) {
            setIntroScreenViewed(true)
        }
        // (jonathan) TODO: Force the app to be portrait on small screens until we have
        // proper landscape layout support
        if (!isLandscapeCapable()) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        installSplashScreen()
        vpnPermissionLauncher =
            registerForActivityResult(VpnPermissionContract()) { granted ->
                if (granted) {
                    TSLog.d("VpnPermission", "VPN permission granted")
                    appViewModel.setVpnPrepared(true)
                    App.get().startVPN()
                } else {
                    if (isAnotherVpnActive(this)) {
                        TSLog.d("VpnPermission", "Another VPN is likely active")
                        showOtherVPNConflictDialog()
                    } else {
                        TSLog.d("VpnPermission", "Permission was denied by the user")
                        appViewModel.setVpnPrepared(false)

                        AlertDialog
                            .Builder(this)
                            .setTitle(R.string.vpn_permission_needed)
                            .setMessage(R.string.vpn_explainer)
                            .setPositiveButton(R.string.try_again) { _, _ ->
                                viewModel.showVPNPermissionLauncherIfUnauthorized()
                            }.setNegativeButton(R.string.cancel, null)
                            .show()
                    }
                }
            }
        viewModel.setVpnPermissionLauncher(vpnPermissionLauncher)
        // Check if device is rooted and alert if so
        // We do not want to abort, but we do want the analyst to be able to decide what to do next
        val rooted = RootUtil.isDeviceRooted
        val builder = AlertDialog.Builder(this)

        setContent {
            if (rooted) {
                builder.setTitle("Device is rooted")
                builder.setMessage("Rooted devices are less secure. If this is expected, please continue; otherwise check with whoever runs the authority node")
                builder.show()
            }

            navController = rememberNavController()

            AppTheme {
                Surface(color = MaterialTheme.colorScheme.inverseSurface) {
                    // Background for the letterbox
                    Surface(modifier = Modifier.universalFit()) {
                        // Letterbox for AndroidTV
                        val startDest = if (isIntroScreenViewedSet()) "meshFlow" else "onboarding"
                        NavHost(
                            navController = navController,
                            startDestination = startDest,
                            enterTransition = {
                                slideInHorizontally(
                                    animationSpec = tween(190, easing = LinearOutSlowInEasing),
                                    initialOffsetX = { it },
                                ) +
                                    fadeIn(
                                        animationSpec =
                                            tween(200, easing = LinearOutSlowInEasing),
                                    )
                            },
                            exitTransition = {
                                slideOutHorizontally(
                                    animationSpec = tween(190, easing = LinearOutSlowInEasing),
                                    targetOffsetX = { -it },
                                ) +
                                    fadeOut(
                                        animationSpec =
                                            tween(200, easing = LinearOutSlowInEasing),
                                    )
                            },
                            popEnterTransition = {
                                slideInHorizontally(
                                    animationSpec = tween(190, easing = LinearOutSlowInEasing),
                                    initialOffsetX = { -it },
                                ) +
                                    fadeIn(
                                        animationSpec =
                                            tween(200, easing = LinearOutSlowInEasing),
                                    )
                            },
                            popExitTransition = {
                                slideOutHorizontally(
                                    animationSpec = tween(190, easing = LinearOutSlowInEasing),
                                    targetOffsetX = { it },
                                ) +
                                    fadeOut(
                                        animationSpec =
                                            tween(200, easing = LinearOutSlowInEasing),
                                    )
                            },
                        ) {
                            // Pop to [route] if present, else pop one level (these screens are also reached from meshFlow).
                            fun backTo(route: String): () -> Unit =
                                {
                                    if (!navController.popBackStack(route = route, inclusive = false)) {
                                        navController.popBackStack()
                                    }
                                }

                            composable("splitTunneling") { SplitTunnelAppPickerView(backTo("settings")) }
                            composable("about") { AboutView(backTo("settings")) }
                            composable("mdmSettings") { MDMSettingsDebugView(backTo("settings")) }
                            composable("notifications") {
                                NotificationsView(backTo("settings"), ::openApplicationSettings)
                            }
                            composable("onboarding") {
                                MeshOnboarding(
                                    onDone = {
                                        setIntroScreenViewed(true)
                                        navController.navigate("meshFlow") {
                                            popUpTo("onboarding") { inclusive = true }
                                        }
                                    },
                                )
                            }
                            composable("ADBSetup") {
                                ADBSetup(
                                    onNavigateHome = {
                                        navController.navigate("meshFlow")
                                    },
                                )
                            }
                            composable("meshFlow") {
                                val ipnState by viewModel.ipnState.collectAsState(initial = Ipn.State.NoState)
                                val vpnActive by appViewModel.vpnActive.collectAsState(initial = false)
                                // Build the UI env once per state change, not on every recomposition.
                                val env = remember(ipnState, vpnActive, adbWifiEnabled) {
                                    MeshLiveEnv(
                                        connected = ipnState == Ipn.State.Running && vpnActive,
                                        analyst = null,
                                        adbReady = adbWifiEnabled,
                                        onStartSession = { showQRScanFromHome = true },
                                        onEndSession = { viewModel.endSession() },
                                        onOpenSplitTunnel = { navController.navigate("splitTunneling") },
                                    )
                                }
                                MeshFlow(env = env, onExit = { moveTaskToBack(true) })
                            }
                            composable("deviceInfo") {
                                DeviceInfoView(backTo("meshFlow"))
                            }
                        }
                    }
                }
                if (showPinDialog) {
                    PinEntryDialog(
                        onConfirm = { pin ->
                            showPinDialog = false
                            pinDialogCallback?.invoke(pin)
                            pinDialogCallback = null
                        },
                        onDismiss = {
                            showPinDialog = false
                            pinDialogCallback = null
                        },
                    )
                }
                if (showQRScanFromHome) {
                    QRScanStep(
                        onScanned = { uri ->
                            showQRScanFromHome = false
                            // A mesh:// link is the provisioning payload of the Tailscale-based
                            // versions, which can no longer be used. Anything else is a MESHnet
                            // server address, paired with the PIN shown on the authority node.
                            if (uri.startsWith("mesh://")) {
                                showUnsupportedLinkDialog()
                            } else {
                                pinInput { pin ->
                                    // IPNService runs the session. It needs VPN
                                    // consent before it can start in the foreground.
                                    MeshnetSession.prepare(uri, pin)
                                    viewModel.showVPNPermissionLauncherIfUnauthorized()
                                }
                            }
                        },
                        onSkip = { showQRScanFromHome = false },
                    )
                }
            }
        }
    }

    private fun showOtherVPNConflictDialog() {
        AlertDialog
            .Builder(this)
            .setTitle(R.string.vpn_permission_denied)
            .setMessage(R.string.multiple_vpn_explainer)
            .setPositiveButton(R.string.go_to_settings) { _, _ ->
                // Intent to open the VPN settings
                val intent = Intent(Settings.ACTION_VPN_SETTINGS)
                startActivity(intent)
            }.setNegativeButton(R.string.cancel, null)
            .show()
    }

    fun isAnotherVpnActive(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        if (activeNetwork != null) {
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            if (networkCapabilities != null &&
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            ) {
                return true
            }
        }
        return false
    }

    private fun pinInput(onPinEntered: (String) -> Unit) {
        pinDialogCallback = onPinEntered
        showPinDialog = true
    }

    private fun showUnsupportedLinkDialog() {
        AlertDialog
            .Builder(this)
            .setTitle("Unsupported code")
            .setMessage("This code is from an older MESH setup and can no longer be used. Scan the code shown on the authority node.")
            .setPositiveButton("Ok", null)
            .show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(START_AT_ROOT, false)) {
            if (this::navController.isInitialized) {
                val previousEntry = navController.previousBackStackEntry
                TSLog.d("MainActivity", "onNewIntent: previousBackStackEntry = $previousEntry")
                if (previousEntry != null) {
                    navController.popBackStack(route = "meshFlow", inclusive = false)
                } else {
                    TSLog.e("MainActivity", "onNewIntent: No previous back stack entry, navigating directly to 'meshFlow'")
                    navController.navigate("meshFlow") { popUpTo("meshFlow") { inclusive = true } }
                }
            }
        }
    }

    // Wireless debugging is the channel the analyst uses to attach over Wi-Fi; read the
    // real OS setting so the Home card reflects actual state, not an in-app guess.
    private fun isWirelessDebuggingOn(): Boolean =
        try {
            Settings.Global.getInt(contentResolver, "adb_wifi_enabled", 0) == 1
        } catch (e: Exception) {
            false
        }

    override fun onResume() {
        super.onResume()
        adbWifiEnabled = isWirelessDebuggingOn()
        val restrictionsManager =
            this.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
        lifecycleScope.launch(Dispatchers.IO) {
            MDMSettings.update(
                App.get(),
                restrictionsManager,
            )
        }
    }

    override fun onStop() {
        super.onStop()
        val restrictionsManager =
            this.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
        lifecycleScope.launch(Dispatchers.IO) {
            MDMSettings.update(
                App.get(),
                restrictionsManager,
            )
        }
    }

    private fun openApplicationSettings() {
        val intent =
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun isIntroScreenViewedSet(): Boolean =
        getSharedPreferences("introScreen", Context.MODE_PRIVATE)
            .getBoolean("seen", false)

    private fun setIntroScreenViewed(seen: Boolean) {
        getSharedPreferences("introScreen", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("seen", seen)
            .apply()
    }
}

@Composable
private fun PinEntryDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false),
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "PIN Required",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Enter the 6-digit PIN shown on the authority node",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { input ->
                        if (input.length <= 6 && input.all { it.isDigit() }) {
                            pin = input
                            isError = false
                        }
                    },
                    singleLine = true,
                    isError = isError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "000000",
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.3f),
                        )
                    },
                )
                if (isError) {
                    Text(
                        text = "Must be exactly 6 digits",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text("Cancel", fontFamily = FontFamily.Monospace)
                    }
                    Button(
                        onClick = {
                            if (pin.length == 6) {
                                onConfirm(pin)
                            } else {
                                isError = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(
                            "Confirm",
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }
    }
}

class VpnPermissionContract : ActivityResultContract<Intent, Boolean>() {
    override fun createIntent(
        context: Context,
        input: Intent,
    ): Intent = input

    override fun parseResult(
        resultCode: Int,
        intent: Intent?,
    ): Boolean = resultCode == Activity.RESULT_OK
}

// /TODO utilise this, this should also move to outside of main
object RootUtil {
    val isDeviceRooted: Boolean
        get() = checkTags() || checkCommonBinaries() || runtimeCheck() || isSELinuxEnforcing()

    private fun checkTags(): Boolean {
        val buildTags = android.os.Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkCommonBinaries(): Boolean {
        val paths =
            arrayOf(
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su",
                "/su/bin/su",
            )
        return paths.any { File(it).exists() }
    }

    private fun runtimeCheck(): Boolean =
        try {
            val process =
                Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            BufferedReader(InputStreamReader(process.inputStream)).use { it.readLine() != null }
        } catch (_: Throwable) {
            false
        }

    private fun isSELinuxEnforcing(): Boolean =
        try {
            val process = Runtime.getRuntime().exec("getenforce")
            val result =
                BufferedReader(InputStreamReader(process.inputStream)).use { it.readLine() }
            result.equals("Enforcing", ignoreCase = true)
        } catch (e: Exception) {
            try {
                val enforceFile = File("/sys/fs/selinux/enforce")
                if (enforceFile.exists()) {
                    enforceFile
                        .readText()
                        .trim() == "1"
                } else {
                    false
                }
            } catch (_: Exception) {
                false
            }
        }
}
