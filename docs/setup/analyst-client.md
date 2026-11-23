# Analyst client setup

The analyst client is `meshcli`, an interactive command-line client that runs in a Docker container on your forensic workstation. It starts the session, admits the device, and runs ADB, acquisition and capture tools over the tunnel.

!!! warning
    Treat the network path to the device as untrusted. The analyst node should be a hardened, dedicated system that can be snapshotted after collection, not your everyday personal workstation. Lateral movement from a mobile device to the analyst container would require a complex attack, but you should still assume the risk exists.

## Step 1: Clone and build

```bash
git clone https://github.com/BARGHEST-ngo/MESHadb.git
cd MESHadb

# Build the analyst image
task build
```

!!! tip "Build time"
    The first build takes a few minutes while it compiles `meshcli` and installs `adb`, `tcpdump`, Suricata and MVT. `task analyst` also builds the image if needed, so this step is optional.

## Step 2: Choose where acquisitions are stored

Acquisitions are written to `/home/mesh/acquisitions` in the container, which is bind-mounted from `${ACQUISITION_DIR:-$HOME/mesh-acquisitions}` on the host. To use a dedicated evidence store, set `ACQUISITION_DIR` in a `.env` file at the repository root:

```bash
# .env
ACQUISITION_DIR=/mnt/evidence/mesh-acquisitions
```

See **[Acquisitions and evidence handling](../user-guide/acquisitions.md)** for details.

## Step 3: Start `meshcli`

```bash
task analyst
```

This starts the analyst container and runs `meshcli` in the foreground. It prints a QR code and the session address, then waits at the `meshcli>` prompt:

```
█▀▀▀▀▀█ ▄▀▄ █▀▀▀▀▀█
...
MESH address: tcomFwWCCcjS5nKNqAod034nWoJZ...

Run 'pin' to pair a device
meshcli>
```

Every start creates new keys and a new address. Nothing is configured or saved between sessions.

Commands are typed at the `meshcli>` prompt without a prefix. Run `help` to list them, or see the [CLI reference](../reference/cli-reference.md).

!!! info "A second terminal"
    For direct `adb`, `tcpdump` or MVT use, open a shell in the running container from another terminal:

    ```bash
    task shell
    ```

## Step 4: Stopping

Type `exit` (or `quit`, or press Ctrl-D) at the prompt to end the session and stop the container. Ctrl-C cancels the running command but does not exit, so a stray keypress cannot drop a connected device.

`task down` stops anything left running.

## Troubleshooting

If you encounter issues during setup, see the [Troubleshooting guide](../reference/troubleshooting.md).

## Next steps

`meshcli` is running and waiting for a device. The next step is to install the app on the Android device.

---

← [Previous: Getting started](index.md) | [Next: Android app](endpoint-client.md) →
