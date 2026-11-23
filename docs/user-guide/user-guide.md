# User guide

This guide covers common forensic workflows and best practices for using MESHadb in real-world investigations.

## Forensic workflows

### Basic Android forensics workflow

1. **Before the session**
    - Agree with the device owner what you will collect and do
    - Make sure the device runs Android 11 or later and has the MESHadb app installed

2. **Start the session**

    ```bash
    task analyst
    ```

    ```
    meshcli> pin
    ```

    Send the address (or QR code) and the PIN to the device owner over separate channels, and wait for `device connected`.

3. **Pair ADB**

    ```
    meshcli> adbpair
    ```

    The device owner turns on Wireless debugging, taps **Pair device with pairing code**, and reads you the code and the ports.

4. **Collect artifacts and run analysis**

    ```
    meshcli> adbcollect
    ```

    Then analyse the acquisition with MVT. See [Acquisitions and evidence handling](acquisitions.md). For a comprehensive guide to Android ADB forensics, see <https://forensics.socialtic.org/how-tos/04-how-to-extract-with-androidqf/index.html>.

5. **Clean up and tear down**, in this order

    ```
    meshcli> adbclean
    meshcli> adbdisable
    meshcli> disconnect
    meshcli> exit
    ```

!!! warning "Order matters"
    `adbclean` needs ADB, and it destroys the device's logs, including those that record MESHadb usage. Run it only after the acquisition, then `adbdisable`.

### Censored network workflow

There is nothing to configure for restrictive networks. When UDP is blocked or hole punching fails, the session automatically falls back to the HTTPS relay on TCP port 443, and moves back to a direct path if one becomes available.

- Expect higher latency and lower throughput while relayed. Large acquisitions take longer
- Keep a backup communication channel with the device owner in case the session drops
- If the session drops, it is over: create a new PIN and have the device rejoin

### Network capture workflow

To see what the device sends over the network, route its traffic through the analyst and record it:

```
meshcli> capture start
meshcli> forward on
... let the device run ...
meshcli> capture stop
meshcli> forward off
```

See [Exit node and packet capture](../advanced/exit-node-pcap.md).

### Several devices

MESHnet currently accepts one device per session. To examine several devices, run them one after another, or run separate sessions on separate analyst machines. Support for several devices in one session is on the roadmap.

## Best practices

### Operational

1. **Get consent first**: Agree the scope with the device owner before the session
2. **Separate channels**: Send the address and the PIN over different channels, at least one end-to-end encrypted
3. **Test before deployment**: Rehearse with a test device
4. **Have backup plans**: Keep an alternative way to reach the device owner
5. **Plan for disconnections**: A dropped session needs a new PIN and a new `adbpair`
6. **Respect privacy**: Only collect what you need
7. **Follow legal requirements**: Ensure compliance with local laws

### Performance

1. **Use Wi-Fi on the device**: Wireless debugging needs it on most devices, and it is usually faster
2. **Collect efficiently**: Use `adbcollect --module` to run a single module when you don't need everything
3. **Expect relay overhead**: Relayed sessions are slower than direct ones

## Common tasks

### Checking the session

```
meshcli> status
```

`status` shows whether a PIN is pending, the connected device and its short ID, and any running capture.

### Running your own ADB commands

`adbpair` leaves the device connected to the analyst's ADB server, so any ADB tool works from `task shell`:

```bash
OUTPUT_DIR="/home/mesh/acquisitions/manual-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUTPUT_DIR"

adb bugreport "$OUTPUT_DIR/bugreport.zip"
adb shell dumpsys > "$OUTPUT_DIR/dumpsys.txt"
adb logcat -d > "$OUTPUT_DIR/logcat.txt"
adb shell pm list packages -f > "$OUTPUT_DIR/packages.txt"
adb shell ps -A > "$OUTPUT_DIR/processes.txt"
adb shell getprop > "$OUTPUT_DIR/properties.txt"
```

Keep output under `/home/mesh/acquisitions` so it lands on the host.

### Re-sharing the address

```
meshcli> qr
meshcli> qr -o /home/mesh/acquisitions/session-qr.png
```

The PNG lands in the acquisitions folder on the host, ready to send.

## Troubleshooting

See the [Troubleshooting guide](../reference/troubleshooting.md) for detailed solutions to common issues.

## Next steps

- **[CLI reference](../reference/cli-reference.md)** - Every `meshcli` command
- **[Exit node and packet capture](../advanced/exit-node-pcap.md)** - Network capture
- **[Troubleshooting](../reference/troubleshooting.md)** - Common issues and solutions
