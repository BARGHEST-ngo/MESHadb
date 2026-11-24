# Your first session

With `meshcli` running and the app installed, this page walks through a full session: join, pair ADB, collect, clean up and disconnect.

## Step 1: Admit the device

At the `meshcli>` prompt, create a PIN:

```
meshcli> pin
PIN: 482913
```

Send the address (or QR code) and the PIN to the device owner over separate channels, and have them join from the app. When they do, `meshcli` prints:

```
device connected: Pixel 8
```

Check the session at any time with `status`:

```
meshcli> status
Pairing: none, run 'pin' to pair a device.
Devices: 1 connected
    "Pixel 8"    a1b2c3d4
```

## Step 2: Pair ADB

Ask the device owner to turn on Wireless debugging (the app guides them), then run:

```
meshcli> adbpair
Using device: Pixel 8

On the device:
1. Turn on Wireless debugging
2. Tap 'Pair device with pairing code'
Pairing code: 123456
Pairing port (the number after ':' under the code): 37155
ADB pair successful
Debug port (Wireless debugging screen, under 'IP address & Port'): 41231
Connecting to device...
Validating ADB session...
Device connected. You may proceed with the acquisition: run 'adbcollect'
```

The device owner reads the pairing code and ports from their screen. Only the port numbers are needed; ignore the IP address shown on the device.

`adbpair` bridges each port through the tunnel to a local port on `127.0.0.1` inside the analyst container, so `adb` sees the device as `127.0.0.1:<port>`.

## Step 3: Test ADB

In a second terminal, open a shell in the analyst container:

```bash
task shell
```

```bash
adb devices
# List of devices attached
# 127.0.0.1:43517    device

adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
```

!!! success "ADB connected"
    If the device is listed, you have remote ADB access over the tunnel.

## Step 4: Run an acquisition

Back at the `meshcli>` prompt:

```
meshcli> adbcollect
```

`adbcollect` runs an AndroidQF/WARD acquisition into a new folder under `/home/mesh/acquisitions`, which is `~/mesh-acquisitions` on the host unless you set `ACQUISITION_DIR`. See [Acquisitions and evidence handling](../user-guide/acquisitions.md) for options and for analysing the result with MVT.

## Step 5: Clean up and disconnect

Run these **after** the acquisition, in this order:

```
meshcli> adbclean
meshcli> adbdisable
meshcli> disconnect
```

- `adbclean` clears logcat buffers and shell-accessible telemetry left by the session. Running it earlier destroys evidence
- `adbdisable` turns off Developer options and Wireless debugging, so the device is not left open to non-consensual extraction
- `disconnect` ends the device's session

Type `exit` to stop `meshcli`.

## Checklist

- [ ] `meshcli` printed `device connected`
- [ ] `adbpair` finished with `Device connected`
- [ ] `adb devices` in `task shell` lists the device
- [ ] `adbcollect` wrote an acquisition to the host
- [ ] `adbclean`, `adbdisable` and `disconnect` ran, in that order

## Troubleshooting

### `adbpair` fails to pair

- The pairing code changes each time the **Pair device with pairing code** dialog opens. Make sure the code and port are from the dialog currently on screen
- The pairing port is the number after `:` **under the code**, not the port on the main Wireless debugging screen

### `adbpair` pairs but fails to connect

- The debug port is the one on the main Wireless debugging screen, under **IP address & Port**. It changes whenever Wireless debugging is toggled
- Check Wireless debugging is still on. Some devices turn it off when Wi-Fi changes

### The device disconnects

A lost connection is a finished session. Run `pin` again and have the device rejoin, then run `adbpair` again.

See the [Troubleshooting guide](../reference/troubleshooting.md) for more.

## Next steps

- **[User guide](../user-guide/user-guide.md)** - Forensic workflows and best practices
- **[Exit node and packet capture](../advanced/exit-node-pcap.md)** - Capture the device's network traffic
- **[CLI reference](../reference/cli-reference.md)** - Every `meshcli` command
- **[Architecture](../overview/architecture.md)** - How MESHadb works under the hood

---

← [Previous: Android app](endpoint-client.md)
