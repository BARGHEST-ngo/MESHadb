# Security model

This page describes what MESHadb protects, what it does not, and how to use it safely.

!!! warning "Not yet audited"
    MESHadb and MESHnet have not yet had an independent security audit. MESHnet is pre-alpha, and its protocol may change.

## What MESHadb protects

### Session content

- **End-to-end encryption** - All traffic between `meshcli` and the app runs inside a WireGuard tunnel (Curve25519, ChaCha20-Poly1305, BLAKE2s), including when it passes through a relay
- **Relays cannot read traffic** - A relay forwards packets that are already encrypted end to end
- **Forward secrecy** - WireGuard rotates session keys, so a later key compromise does not expose earlier traffic

### Admission

- **The address is a secret** - The address carries the analyst's public key and the relay to meet at. A device with only the address cannot join
- **Single-use PINs** - A PIN admits one device and expires after 10 minutes
- **Limited guessing** - Three wrong PINs cancel the pairing
- **Key binding** - The analyst checks that the device presenting the PIN is the one on the other end of the tunnel

### Device exposure

- **ADB only** - The app accepts only ADB connections from the analyst. It refuses any other kind of stream
- **Forwarding is opt-in** - The analyst grants forwarding per device, and it is off by default
- **Local network stays local** - Forwarding never reaches private, link-local or carrier-grade NAT addresses through the analyst

### Ephemerality

- **New keys every start** - `meshcli` creates new keys and a new address every time. The app uses new keys for every session
- **Nothing persists** - No accounts, no saved state. A lost connection is a finished session, and the device must rejoin with a new PIN

## What MESHadb does not protect

- **Metadata** - Relays can see both ends' IP addresses, connection timing and data volume
- **IP addresses** - Once connected, each end can learn the other's IP address. MESHadb is not an anonymity tool
- **The fact of a session** - A network observer can see that a device connected to a relay, even though it cannot read the content
- **A compromised analyst** - Whoever controls `meshcli` has ADB access to the device for the session. Run it on a hardened, dedicated machine
- **What happens over ADB** - With ADB, the analyst can read logs, copy files, install apps and change settings. The device owner's consent is the control here

### Relays

The current release uses tailcat's default relay map, which points at free, rate-limited [DERP](https://github.com/tailscale/tailscale/tree/main/cmd/derper#derp) relays run by Tailscale. They are used to set up every session and carry its traffic when no direct path is possible. They cannot decrypt traffic, but they see the metadata listed above. Choosing your own relays is not yet configurable.

## Using MESHadb safely

1. **Share the address and PIN separately** - Use two channels, at least one of them end-to-end encrypted, such as Signal
2. **Get consent** - Agree with the device owner what you will do before the session. The person holding the phone installs the app, enters the PIN and enables Wireless debugging themselves. Only use MESHadb on devices whose owner has agreed
3. **Use a dedicated analyst machine** - Treat the device as untrusted, and keep the analyst separate from your everyday workstation
4. **Protect acquisitions** - Point `ACQUISITION_DIR` at an encrypted volume. Acquisitions hold pulled APKs and a device backup
5. **Clean up in order** - After the acquisition, run `adbclean`, then `adbdisable`, then `disconnect`
6. **End sessions promptly** - Exit `meshcli` when you are done, so the address stops working

## After the session

- `adbdisable` turns off Developer options and Wireless debugging, so the device is not left open to non-consensual extraction
- `adbclean` clears logcat buffers and shell-accessible telemetry. It is best effort: the paired ADB key, Wireless debugging history and root-only logs remain on the device. Only a factory reset clears the paired-keys store

## Reporting vulnerabilities

Please report security issues privately. See [SECURITY.md](https://github.com/BARGHEST-ngo/MESHadb/blob/main/SECURITY.md).
