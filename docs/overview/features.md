# Features

MESHadb provides a focused set of features for secure remote mobile forensics in challenging network environments.

## End-to-end encryption

All traffic between `meshcli` and the Android app runs inside a WireGuard tunnel.

- **Fresh keys every session** - `meshcli` generates new keys, and so a new address, every time it starts. The app generates new keys for every session
- **No key exchange to manage** - The address carries the public key and the relay to meet at. There are no keys to copy and no endpoints to configure
- **Industry-standard cryptography** - WireGuard's Noise protocol: Curve25519, ChaCha20-Poly1305 and BLAKE2s, with forward secrecy
- **Relays cannot read traffic** - Relayed packets are still WireGuard-encrypted end to end

!!! note "What relays can see"
    A relay cannot decrypt traffic, but it can see the IP addresses of both ends, timing, and data volume. Once connected, each end can also learn the other's IP address. MESHadb is not an anonymity tool. See the [Security model](../reference/security.md).

## Peer-to-peer connections

- **Direct first** - Both ends meet at a relay, exchange their network endpoints, and punch through NAT to a direct UDP path
- **No open ports** - No port forwarding, router changes or administrator access on either side
- **Automatic upgrade** - A relayed session moves to a direct path as soon as one becomes available

## Censorship resistance

- **HTTPS relay fallback** - When UDP or WireGuard is blocked, for example by deep packet inspection, traffic falls back to a [DERP](https://github.com/tailscale/tailscale/tree/main/cmd/derper#derp) relay over HTTPS on TCP port 443
- **Looks like web traffic** - To the network in between, a relayed session is an HTTPS connection
- **Circumvention** - With [forwarding](../advanced/exit-node-pcap.md) on, a device on a censored network reaches the internet from the analyst's location

!!! info "Traffic obfuscation"
    Earlier versions of MESH used AmneziaWG to disguise WireGuard packets. The current transport, MESHnet, does not. Its censorship resistance comes from the HTTPS relay fallback.

## Mobile forensics

### Remote ADB

- **Wireless debugging over the tunnel** - `adbpair` pairs with the device's Wireless debugging and bridges it to a local port on the analyst, so `adb` works as if the phone were on your desk
- **No USB cable** - The device owner enables Wireless debugging and reads out the pairing code
- **Restricted to ADB** - The app only accepts ADB connections from the analyst; it refuses any other stream

### Forensic tool integration

- **AndroidQF and WARD** - Built into `adbcollect`
- **MVT (Mobile Verification Toolkit)** - Installed in the analyst container for offline analysis
- **Any ADB tool** - Open `task shell` to run `adb` directly against the bridged device

!!! example "Example: remote acquisition"
    ```text
    meshcli> adbpair
    meshcli> adbcollect
    ```

    Then, from `task shell`:

    ```bash
    mvt-android check-androidqf /home/mesh/acquisitions/<acquisition-folder>
    ```

### Clean-up after the session

- **`adbclean`** - Clears logcat buffers and shell-accessible telemetry left by the session (best effort)
- **`adbdisable`** - Turns off Developer options and Wireless debugging, so the device is not left open to non-consensual extraction

## Network forensics

### Forwarding (exit node)

- `forward on` sends the device's TCP and UDP internet traffic through the tunnel to the analyst, which sends it on to the internet
- Local network destinations stay local and are never forwarded
- `forward off` returns the device to its own connection

### Packet capture

- `capture start` records forwarded traffic to a pcap in the device's acquisition folder
- `tcpdump` and Suricata ship in the analyst image for your own captures and analysis

See [Exit node and packet capture](../advanced/exit-node-pcap.md).

## Rapid deployment

- **Nothing to deploy** - No control plane, no accounts, no server. Start `meshcli`, create a PIN, and the device can join
- **Ephemeral sessions** - Nothing is persisted. A lost connection is a finished session, and the device rejoins with a new PIN
- **Containerised** - The analyst client runs in Docker with all its tools, started with a single `task analyst`

## Security and consent

- **Single-use PINs** - A PIN admits one device, expires after 10 minutes, and three wrong guesses cancel the pairing
- **The address is a secret** - A device needs both the address and the PIN to join
- **Consent by design** - Only the person holding the phone can install the app, enter the PIN and enable Wireless debugging
- **Forwarding is opt-in** - The analyst grants forwarding per device. It is off by default

!!! note "One device per session"
    MESHnet currently accepts one device at a time. Support for several devices in one session is on the roadmap.

---

**Next:** Explore the [Architecture](architecture.md) to understand how these features are implemented →
