// Copyright (c) Tailscale Inc & contributors
// SPDX-License-Identifier: BSD-3-Clause
//
// Portions Copyright (c) BARGHEST
// SPDX-License-Identifier: AGPL-3.0-or-later
//
// This file contains code originally from Tailscale (BSD-3-Clause)
// with modifications by BARGHEST. The modified version is licensed
// under AGPL-3.0-or-later. See LICENSE for details.

package com.barghest.mesh.ui.model

class Ipn {

  // The session state the UI reads. MeshnetSession translates MESHnet's states into it.
  enum class State(val value: Int) {
    NoState(0),
    InUseOtherUser(1),
    NeedsLogin(2),
    NeedsMachineAuth(3),
    Stopped(4),
    Starting(5),
    Running(6),
    // Stopping represents a state where a request to stop has been issued but has not
    // completed.
    Stopping(7)
  }
}
