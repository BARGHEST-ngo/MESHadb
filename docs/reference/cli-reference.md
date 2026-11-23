# CLI reference

`meshcli` is an interactive client. `task analyst` starts it in the analyst container, and you type commands at its prompt without a prefix:

```
meshcli> <command> [flags] [arguments]
```

On start, `meshcli` creates a new session: new keys, a new address, and a QR code of that address. The session ends when `meshcli` exits.

## Prompt behaviour

- `help` lists the commands. `<command> --help` shows a command's flags
- `exit`, `quit` or Ctrl-D stops `meshcli` and ends the session
- Ctrl-C cancels the running command, or clears the prompt. It never exits `meshcli`, so a stray keypress cannot drop a connected device
- Messages such as `device connected: <name>` and `device disconnected: <name>` appear as events happen

## Device selection

Commands that act on a device (`adbpair`, `adbcollect`, `adbclean`, `adbdisable`, `forward`, `capture start`, `disconnect`) use the connected device automatically. If none is connected they fail with `no device connected`. If several are connected, they ask you to choose.

!!! note
    MESHnet currently accepts one device per session, so you will normally see only one.

## Commands

### pin

Create a one-time PIN so a device can join.

```
meshcli> pin
PIN: 482913
```

- A PIN admits exactly one device and expires after 10 minutes
- Three wrong guesses cancel the pairing
- If `pin` reports `too many failed pairing attempts, restart MESH`, exit and start `meshcli` again. This creates a new address
- If it reports `server at capacity`, a device is already connected

### qr

Show the session address as a QR code, or save it as a PNG.

```
qr [-o file.png]
```

| Flag | Description |
|------|-------------|
| `-o <file>` | Save the QR code as a 512×512 PNG to this path instead of printing it |

```
meshcli> qr
meshcli> qr -o /home/mesh/acquisitions/session-qr.png
```

Save under `/home/mesh/acquisitions` to find the file on the host.

### status

Show the session: whether a PIN is waiting to be used, the connected devices with a short ID, and any running capture.

```
meshcli> status
Pairing: pending (PIN: 482913)
Devices: 1 connected
    "Pixel 8"    a1b2c3d4
Capture: running, writing to /home/mesh/acquisitions/<uuid>/capture-2026-11-23T101500Z.pcap
```

### adbpair

Pair with the device's Wireless debugging and connect `adb` to it. Takes no flags; it asks for each value.

```
meshcli> adbpair
```

It asks for:

1. **Pairing code** - The 6-digit code from **Pair device with pairing code** on the device
2. **Pairing port** - The number after `:` under that code
3. **Debug port** - The port on the main Wireless debugging screen, under **IP address & Port**

`adbpair` bridges each port through the tunnel to a local port on `127.0.0.1`, runs `adb pair` and `adb connect` against them, and checks that the device appears in `adb devices`. The connection then serves `adbcollect`, `adbclean`, `adbdisable`, and any `adb` command you run from `task shell`.

### adbcollect

Run an ADB acquisition with AndroidQF and WARD. The output is ready for WARD or MVT.

```
adbcollect [flags]
```

| Flag | Description |
|------|-------------|
| `--output <dir>` | Write the acquisition here. Default: the device's folder under `/home/mesh/acquisitions` |
| `--module <name>` | Run a single module, for example `BackupTar` |
| `--list <any>` | List available modules and exit. Takes a value, for example `--list all` |
| `--fast <any>` | Fast mode: skip some checks. Takes a value, for example `--fast yes` |
| `--verbose` | Verbose output |
| `--version` | Show the AndroidQF version and exit |

```
meshcli> adbcollect
meshcli> adbcollect --module BackupTar
meshcli> adbcollect --output /home/mesh/acquisitions/case-01
```

- Needs a successful `adbpair` first, otherwise it fails with `device not connected over ADB, run adbpair`
- Times out after 60 minutes
- The first `adbcollect` for a device shares its folder with any `capture` pcap. Later runs get a new folder
- Keep `--output` under `/home/mesh/acquisitions`, or the acquisition is lost when `meshcli` exits

See [Acquisitions and evidence handling](../user-guide/acquisitions.md).

### forward

Route the device's internet traffic through the analyst, or stop.

```
forward on|off
```

```
meshcli> forward on
requested Pixel 8 to enable network forwarding on
Pixel 8: forwarding true
```

- The app asks the device owner for VPN permission the first time
- Local network destinations are never forwarded
- Fails with `a capture is already running` if another device is being captured

See [Exit node and packet capture](../advanced/exit-node-pcap.md).

### capture

Record the device's forwarded traffic to a pcap with `tcpdump`, running in the background.

```
capture start [-i <interface>]
capture stop
```

| Flag | Description |
|------|-------------|
| `-i <interface>` | Interface to capture on. Default: `eth0` |

- The pcap is written to the device's acquisition folder as `capture-<UTC time>.pcap`
- Only one capture runs at a time, and while it runs only the captured device may forward
- `capture stop` prints the path and tcpdump's packet summary
- The capture stops automatically if the device disconnects or `meshcli` exits

### adbclean

Clear logs and shell-accessible telemetry left by the session. Best effort.

```
meshcli> adbclean
```

It resets battery and process statistics, clears the statsd puller cache, and clears all logcat buffers.

!!! danger "Run only after the acquisition"
    `adbclean` destroys the device's logs, including evidence. Run it after `adbcollect`, and before `adbdisable`, which removes ADB access.

It does **not** remove the paired ADB key, Wireless debugging history, or root-only logs such as tombstones, dropbox and pstore. Only a factory reset clears the paired-keys store.

### adbdisable

Turn off Developer options and Wireless debugging on the device, so it is not left open to non-consensual extraction.

```
meshcli> adbdisable
```

It waits for the device to drop ADB, then disconnects `adb`. The device stays connected to the session; run `disconnect` to end it.

### disconnect

End the device's session.

```
meshcli> disconnect
```

The tunnel closes and any ADB bridges and running capture for the device stop. The device must rejoin with a new PIN.

## Host commands

Run these from the repository root on the host:

| Command | Description |
|---------|-------------|
| `task analyst` | Build the image if needed, start the analyst container and run `meshcli`. Alias of `task node` |
| `task shell` | Open a shell in the running container as the `mesh` user, for `adb`, `tcpdump`, Suricata and MVT |
| `task build` | Build the analyst image |
| `task down` | Stop the analyst container |

### Environment variables

Read from `.env` at the repository root:

| Variable | Description |
|----------|-------------|
| `ACQUISITION_DIR` | Host directory mounted at `/home/mesh/acquisitions`. Default: `$HOME/mesh-acquisitions` |

## Next steps

- **[User guide](../user-guide/user-guide.md)** - Forensic workflows
- **[Troubleshooting](troubleshooting.md)** - Common issues and solutions
