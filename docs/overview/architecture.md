# Architecture

MESHadb has two components: the **analyst client** (`meshcli`) on your computer and the **Android app** on the device. They connect over [MESHnet](https://github.com/BARGHEST-ngo/MESHnet), a peer-to-peer WireGuard protocol with no control plane. A public relay helps the two ends find each other and carries traffic only when a direct path is impossible.

```
  analyst (your computer)                               Android device
┌────────────────────────┐  1. direct: WireGuard/UDP  ┌──────────────────┐
│ adb ─► meshcli         │ ◄════════════════════════► │  MESHadb app     │
│        (Authority)     │                            │  (Peer) ─► adbd  │
└───────────┬────────────┘                            └─────────┬────────┘
            │       2. fallback: HTTPS relay (TCP 443)          │
            └────────────────►   DERP relay   ◄─────────────────┘
                          (sees encrypted packets only)
```

## Components

### Analyst client (`meshcli`)

`meshcli` runs in a Docker container on your forensic workstation. In MESHnet terms it is the **Authority**: it creates the session, issues PINs and decides what each device may do.

- Starts a MESHnet server with new keys and prints the session address as text and a QR code
- Issues single-use PINs with `pin`
- Bridges a local port to the device's Wireless debugging port, so `adb` can reach it (`adbpair`)
- Runs acquisitions with AndroidQF/WARD (`adbcollect`) and clean-up (`adbclean`, `adbdisable`)
- Grants or revokes forwarding of the device's internet traffic (`forward`) and records it (`capture`)

The container also ships `adb`, `tcpdump`, Suricata and MVT. See [Runtime stack](../reference/build-deploy/runtime-stack.md).

### Android app

The app is the MESHnet **Peer**. It runs on the device being examined.

- Joins a session by scanning the address QR code and entering the PIN
- Guides the user to turn on Wireless debugging
- Accepts ADB connections from the analyst and proxies them to the device's local Wireless debugging port. It refuses any other kind of connection
- When the analyst turns forwarding on, uses Android's VPN API to send the device's internet traffic through the tunnel

Android 11 or later is needed for Wireless debugging.

### Relay

The relay is a meeting point and a fallback, not a hub. MESHnet uses [DERP](https://github.com/tailscale/tailscale/tree/main/cmd/derper#derp) relays, via [tailcat](https://github.com/tailscale/tailcat).

- Both ends first connect to the relay named in the session address
- They swap their network endpoints over it and try to punch a direct UDP path
- If that fails, traffic stays on the relay over HTTPS on TCP port 443, still WireGuard-encrypted

!!! warning "Who runs the relays"
    The current release uses tailcat's default relay map, which points at free, rate-limited relays run by Tailscale. They cannot decrypt traffic, but they can see both ends' IP addresses, timing and data volume. Choosing your own relays is not yet configurable.

## A session, step by step

1. **Start** - `meshcli` starts a MESHnet server with new keys and connects to a relay. Its address holds its public key and the relay to meet at. It does **not** contain the analyst's IP address.
2. **Create a PIN** - `pin` creates a 6-digit PIN that admits exactly one device. It expires after 10 minutes.
3. **Share** - The analyst sends the address (or QR code) and the PIN to the device owner, over separate channels.
4. **Join** - The app meets the analyst at the relay and the WireGuard tunnel comes up through it. Inside the tunnel, the app presents the PIN, and the analyst checks it and checks that the device's key matches the tunnel.
5. **Find a direct path** - In parallel, both ends try UDP hole punching. If it works, traffic moves to the direct path; if not, it stays on the relay.
6. **Pair ADB** - `adbpair` opens local ports on the analyst and bridges each one, through the tunnel, to a port on the device. `adb pair` and `adb connect` then talk to those local ports.
7. **Work** - `adbcollect`, `adb shell`, `forward` and `capture` all run over the tunnel.
8. **End** - `disconnect`, closing the app, or stopping `meshcli` ends the session. Nothing is persisted; a new session needs a new PIN.

## Network details

### Addressing

Devices do not get a routable mesh IP address. ADB reaches the device through named streams inside the tunnel, which `meshcli` exposes as local ports on `127.0.0.1` inside the analyst container. `adb devices` therefore lists the device as `127.0.0.1:<port>`.

### NAT traversal

- **Relay-assisted endpoint discovery** - Both ends learn and exchange their public endpoints through the relay
- **UDP hole punching** - Both ends send to each other at once to open a path through NAT
- **Relay fallback** - When hole punching fails, for example behind symmetric or carrier-grade NAT, traffic stays on the relay

### Forwarding

When the analyst runs `forward on`, the app creates a VPN interface on the device and routes its internet traffic into the tunnel. The analyst's MESHnet server opens each TCP and UDP connection onward from the analyst's own network, so destination servers see the analyst's IP address. Private, link-local and carrier-grade NAT destinations are refused, so the device's local network is never reachable through the analyst. DNS queries use `1.1.1.1` while forwarding is on.

## Security model

- **Mutual authentication** - The PIN admits a device, and the analyst checks that the device's key matches the tunnel it presented the PIN on
- **Encryption** - WireGuard (Curve25519, ChaCha20-Poly1305, BLAKE2s) end to end, including over the relay
- **Least privilege** - The app only accepts ADB streams, and forwarding is off unless the analyst grants it
- **Ephemeral** - New keys every start; single-use, time-limited PINs; no saved state

See the [Security model](../reference/security.md) for the full picture, including what MESHadb does not protect against.

## Deployment model

One analyst runs `meshcli` and one device joins its session. MESHnet currently accepts one device at a time; several devices per session is on the roadmap. Several analysts can each run their own independent session.

---

**Next:** Learn about real-world [Use cases](use-cases.md) →
