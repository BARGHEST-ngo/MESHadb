# Troubleshooting

Common issues with MESHadb sessions, ADB and the Android app. For build and container issues, see [Build troubleshooting](build-deploy/troubleshooting.md).

## Starting `meshcli`

### `start:` error when `meshcli` starts

**Symptoms:** `task analyst` prints `start: <error>` and exits.

**Solutions:**

1. **Check internet access** - `meshcli` must reach a relay over HTTPS (TCP 443) to start. Check the host is online and that outbound HTTPS is allowed
2. **Check DNS and proxies** - Corporate proxies that intercept TLS can block the relay connection. Try another network
3. **Try again** - Transient relay or network failures usually clear on a second attempt

### `task analyst` fails before `meshcli` starts

See [Build troubleshooting](build-deploy/troubleshooting.md).

## Joining

### The device can't join

**Symptoms:** The app reports an error after entering the PIN, or `meshcli` never prints `device connected`.

**Solutions:**

1. **PIN expired or used** - A PIN expires after 10 minutes and admits one device. Run `pin` for a new one
2. **Wrong address** - Rescan the QR code, or check the address was copied in full. `qr` shows it again
3. **`meshcli` was restarted** - Every start creates a new address. Send the new address
4. **Device offline** - Check the device has internet access

### `pin` says too many failed pairing attempts

Three wrong PINs cancel pairing for the session. Exit `meshcli` and start it again; this creates a new address, so send it to the device owner again.

### `pin` says the server is at capacity

A device is already connected. MESHnet accepts one device per session. Run `disconnect` first if you want to admit a different device.

## Connectivity

### The session is slow

**Symptoms:** Transfers and `adb shell` respond slowly.

**Causes:** The session is probably running over the relay rather than a direct path. This happens when:

- Both ends are behind restrictive NAT, such as carrier-grade NAT or symmetric NAT
- A network blocks UDP, for example hotel, corporate or censored networks

**Solutions:**

1. **Change network on the device** - Moving the device to a different Wi-Fi network often allows a direct path
2. **Allow outbound UDP on the analyst** - A firewall on the analyst's network that blocks UDP forces the relay
3. **Accept it** - The relay is rate-limited but works. Allow more time for large acquisitions, or use `adbcollect --module` to collect only what you need

### The device disconnects

**Symptoms:** `meshcli` prints `device disconnected: <name>`.

A lost connection is a finished session. Nothing persists, so:

1. Run `pin` and have the device rejoin
2. Run `adbpair` again
3. Re-run any interrupted `adbcollect`

To make drops less likely:

- Exempt the app from battery optimisation on the device (Settings → Apps → MESH → Battery)
- Keep the app open, and the screen on, during long acquisitions
- Avoid switching networks on the device during the session

## ADB issues

### `adbpair` fails to pair

**Symptoms:** `ADB pair failed`.

1. **Stale code** - The pairing code and port change every time the **Pair device with pairing code** dialog opens. Use the values on screen now
2. **Wrong port** - The pairing port is the number after `:` under the code, not the port on the main Wireless debugging screen
3. **Dialog closed** - Keep the pairing dialog open on the device until pairing succeeds

### `adbpair` pairs but cannot connect

**Symptoms:** `ADB connect failed`, or `device not connected through MESH`.

1. **Wrong debug port** - Use the port on the main Wireless debugging screen, under **IP address & Port**
2. **Wireless debugging turned off** - Some devices turn it off when Wi-Fi changes or the screen locks. Turn it on again; the debug port will change
3. **Authorisation prompt** - Check the device for an "Allow wireless debugging?" prompt and accept it

### `device not connected over ADB, run adbpair`

`adbcollect`, `adbclean` and `adbdisable` need a working `adbpair` first. If the device rejoined or Wireless debugging was toggled, run `adbpair` again.

### `adbcollect` fails part-way

1. **Check the device is still connected** - `status`
2. **Check ADB** - In `task shell`, `adb devices` should list `127.0.0.1:<port>` as `device`, not `offline`
3. **Run again** - A second run writes to a new folder, so the first is kept
4. **Verbose output** - `adbcollect --verbose` shows which module failed

## Android app issues

### App crashes

1. **Clear app data** - Settings → Apps → MESH → Storage → Clear data, then rejoin
2. **Reinstall the app**
3. **Collect logs** over USB, if available:

    ```bash
    adb logcat -d > android-logs.txt
    ```

### VPN permission

Forwarding needs the app's VPN permission. If it was declined:

1. Check no other VPN app is active, since only one can run
2. Have the device owner allow the VPN connection for the app, then run `forward on` again

### Wireless debugging is missing

- The device must run Android 11 or later
- Developer options must be enabled first (tap **Build number** 7 times)
- Managed devices and some custom ROMs disable it

## Diagnostics

From the host:

```bash
# Container state
docker ps --filter name=mesh-analyst

# Image build output
task build
```

From `task shell`:

```bash
# ADB view of the device
adb devices -l

# Analyst internet access
ping -c 3 1.1.1.1
```

## Getting help

If you can't resolve your issue:

1. **Check GitHub Issues**: [github.com/BARGHEST-ngo/MESHadb/issues](https://github.com/BARGHEST-ngo/MESHadb/issues)
2. **Open a bug report**: Include the `meshcli` output, Android version and device model, and steps to reproduce. Do not include the session address or PIN
3. **Contact BARGHEST**: [barghest.asia](https://barghest.asia)

## Next steps

- **[CLI reference](cli-reference.md)** - Every `meshcli` command
- **[Build troubleshooting](build-deploy/troubleshooting.md)** - Container and build issues
