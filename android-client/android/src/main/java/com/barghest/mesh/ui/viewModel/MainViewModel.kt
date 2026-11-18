// Copyright (c) Tailscale Inc & contributors
// SPDX-License-Identifier: BSD-3-Clause
//
// Portions Copyright (c) BARGHEST
// SPDX-License-Identifier: AGPL-3.0-or-later
//
// This file contains code originally from Tailscale (BSD-3-Clause)
// with modifications by BARGHEST. The modified version is licensed
// under AGPL-3.0-or-later. See LICENSE for details.
package com.barghest.mesh.ui.viewModel

import android.content.Intent
import android.net.VpnService
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.barghest.mesh.App
import com.barghest.mesh.UninitializedApp
import com.barghest.mesh.ui.model.Ipn
import com.barghest.mesh.ui.notifier.Notifier
import com.barghest.mesh.util.TSLog

class MainViewModelFactory(private val appViewModel: AppViewModel) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
      return MainViewModel(appViewModel) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}

class MainViewModel(private val appViewModel: AppViewModel) : ViewModel() {
  // The session state, published by MeshnetSession.
  val ipnState = Notifier.state

  // Permission to prepare VPN
  private var vpnPermissionLauncher: ActivityResultLauncher<Intent>? = null

  fun setVpnPermissionLauncher(launcher: ActivityResultLauncher<Intent>) {
    vpnPermissionLauncher = launcher
  }

  // Asks for VPN consent if it has not been given, otherwise starts the session service.
  fun showVPNPermissionLauncherIfUnauthorized() {
    val vpnIntent = VpnService.prepare(App.get())
    TSLog.d("VpnPermissions", "vpnIntent=$vpnIntent")
    if (vpnIntent != null) {
      vpnPermissionLauncher?.launch(vpnIntent)
    } else {
      appViewModel.setVpnPrepared(true)
      UninitializedApp.get().startVPN()
    }
  }

  fun endSession() {
    if (Notifier.state.value == Ipn.State.Running) {
      UninitializedApp.get().stopVPN()
    }
  }
}
