# Android app setup

The MESHadb app runs on the device being examined. It joins the analyst's session and lets the analyst reach the device's Wireless debugging over the encrypted tunnel. On the device, the app is labelled **MESH**.

!!! info "Current support is limited to Android"
    Remote ADB needs Android 11 or later, the first version with Wireless debugging.

## Step 1: Install the app

The device owner installs the app from one of:

- [Google Play](https://play.google.com/store/apps/details?id=com.barghest.mesh)
- [F-Droid](https://f-droid.org/en/packages/com.barghest.mesh/)
- The APK on [GitHub Releases](https://github.com/BARGHEST-ngo/MESHadb/releases)

Installing from a store is simplest for a remote device owner. If you use the APK, they will need to allow installation from their browser or file manager when Android asks, and should turn that permission off again afterwards.

### Building from source

```bash
make -C android-client apk
```

The build downloads the pinned MESHnet Android library and checks its SHA-256 before compiling. See [Build orchestration](../reference/build-deploy/build-orchestration.md) for release builds.

## Step 2: Join the session

Before the device owner joins, agree with them on what you will do during the session. The app walks them through this consent step the first time it opens.

1. On the analyst, run `pin` at the `meshcli>` prompt
2. Send the device owner the session address or QR code, and the PIN, over separate channels, at least one of them end-to-end encrypted such as Signal
3. The device owner opens the app, scans the QR code (or enters the address), and enters the PIN when asked
4. When the device joins, `meshcli` prints `device connected: <device name>`

!!! warning "PINs are single-use"
    A PIN admits one device and expires after 10 minutes. Three wrong guesses cancel the pairing. If the device drops off later, it must rejoin with a new PIN.

## Step 3: Turn on Wireless debugging

The app shows a **Set up wireless debugging** card that guides the device owner:

1. Open **Settings → About phone**
2. Tap **Build number** 7 times, entering the device PIN if asked
3. Open **Settings → System → Developer options**
4. Switch on **Wireless debugging**

The app's **Open settings** button jumps straight to Developer options. Wireless debugging needs a Wi-Fi connection on most devices.

## Step 4: Grant VPN permission (only for forwarding)

If you later run `forward on` to route the device's traffic through the analyst, Android asks the device owner to allow the app to set up a VPN connection. Tap **OK**. Without it, forwarding cannot start. See [Exit node and packet capture](../advanced/exit-node-pcap.md).

## Ending the session

The device owner can end the session at any time from the app. The analyst can end it with `disconnect`, or by stopping `meshcli`. Either way the tunnel is gone, and a new session needs a new PIN.

## Troubleshooting

### The app won't install

- Check the device has enough free storage
- Uninstall any previous version first
- If installing the APK, check that installation from that source is allowed

### The device can't join

- Check the PIN has not expired (10 minutes) or already been used. Run `pin` again for a new one
- Check the address was copied in full, or rescan the QR code. `qr` at the `meshcli>` prompt shows it again, and `qr -o file.png` saves it as an image
- Check the device has internet access

### Wireless debugging is missing

- Make sure the device runs Android 11 or later
- Some managed or custom ROM devices disable Developer options. Check with the device owner or their administrator

See the [Troubleshooting guide](../reference/troubleshooting.md) for more.

## Next steps

The app is installed and the device owner knows how to join. Next, run your first session end to end.

---

← [Previous: Analyst client](analyst-client.md) | [Next: First session](verification.md) →
