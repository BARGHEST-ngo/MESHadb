# Platform support

This page details current support and planned features.

## Current support (public beta)

| Component | Android | iOS | Linux | macOS | Windows |
|-----------|---------|-----|-------|-------|---------|
| **Analyst client** (`meshcli`) | N/A | N/A | ✅ | ✅ (Docker) | ⚠️ Untested (Docker/WSL) |
| **Android app** | ✅ Android 11+ | ❌ Planned | N/A | N/A | N/A |

### Android app

**Supported versions:**

- The app installs on Android 8.0 (Oreo) and later
- Remote ADB needs **Android 11 or later**, the first version with Wireless debugging

**Features:**

- Join a session by QR code and PIN
- Guided setup of Wireless debugging
- Forwarding of the device's internet traffic when the analyst turns it on
- Connection status UI

**Requirements:**

- Internet access (Wi-Fi or mobile data)
- Wireless debugging needs a Wi-Fi connection on most devices
- VPN permission, used when the analyst turns forwarding on

**Installation:**

- [Google Play](https://play.google.com/store/apps/details?id=com.barghest.mesh)
- [F-Droid](https://f-droid.org/en/packages/com.barghest.mesh/)
- APKs on [GitHub Releases](https://github.com/BARGHEST-ngo/MESHadb/releases)
- Build from source (see [Android app](../setup/endpoint-client.md))

### Analyst client

**Supported platforms:**

- Any system that runs Docker with the Compose V2 plugin
- Developed and tested on Linux

**Requirements:**

- Docker Engine with `docker compose`
- [Task](https://taskfile.dev/installation/)
- Outbound internet access. No inbound ports are needed

**Installation:**

- Docker Compose (see [Analyst client](../setup/analyst-client.md))

## Planned support

| Platform | Role | Status |
|----------|------|--------|
| **iOS** | Device app | Planned |

### iOS (device app)

**Planned features:**

- Join a session by QR code and PIN
- libimobiledevice integration
- Connection status UI

!!! info "iOS development"
    Follow progress on [GitHub](https://github.com/BARGHEST-ngo/MESHadb) for updates.

## Compatibility notes

### Android

**Known issues:**

- Some OEMs (Xiaomi, Huawei and others) stop background apps aggressively, which can drop the session
- Wireless debugging may be missing or disabled on some custom ROMs and managed devices
- Only one VPN can be active at a time, so another VPN app blocks forwarding

**Workarounds:**

- Exempt the MESHadb app from battery optimisation
- Keep the app open during the session
- Disconnect other VPN apps before turning forwarding on

---

**Next:** Learn [About BARGHEST](about.md) and the team behind MESHadb →
