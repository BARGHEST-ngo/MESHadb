// Copyright (c) Tailscale Inc & contributors
// SPDX-License-Identifier: BSD-3-Clause
//
// Portions Copyright (c) BARGHEST
// SPDX-License-Identifier: AGPL-3.0-or-later
//
// This file contains code originally from Tailscale (BSD-3-Clause)
// with modifications by BARGHEST. The modified version is licensed
// under AGPL-3.0-or-later. See LICENSE for details.

package com.barghest.mesh.ui.notifier

import com.barghest.mesh.ui.model.Ipn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Notifier holds the session state the UI and the status notification read.
// MeshnetSession and IPNService publish to it through setState.
object Notifier {
  private val _state = MutableStateFlow(Ipn.State.NeedsLogin)
  val state: StateFlow<Ipn.State> = _state

  fun setState(newState: Ipn.State) {
    _state.value = newState
  }
}
