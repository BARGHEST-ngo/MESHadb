# Use cases

MESHadb is designed for forensic investigators, human rights defenders, and security researchers operating in challenging environments. Here are real-world scenarios where it provides critical capabilities.

## Remote mobile forensics

Investigate Android devices remotely without physical access. Collect artifacts, run spyware checks, and perform live analysis over an encrypted tunnel.

### Scenario

A human rights organisation needs to analyse the phone of an activist who suspects they've been targeted with spyware. The activist is in a different country and cannot safely mail their device.

### How MESHadb helps

1. **Start a session** - The investigator runs `meshcli` and creates a PIN
2. **Install the app** - The activist installs MESHadb from Google Play, F-Droid or GitHub
3. **Join** - The activist scans the QR code and enters the PIN, received over Signal
4. **Pair ADB** - The activist turns on Wireless debugging and reads out the pairing code; the investigator runs `adbpair`
5. **Collect** - `adbcollect` runs an AndroidQF/WARD acquisition; MVT analyses it offline
6. **Clean up** - `adbclean`, then `adbdisable`, then `disconnect`

### Benefits

- **No physical access required** - Investigate devices anywhere in the world
- **Secure data transfer** - All forensic data is encrypted end to end
- **Minimal user interaction** - Install an app, enter a PIN, flip one setting
- **No infrastructure** - Nothing to deploy or maintain between cases

## Network monitoring and packet capture

Observe a device's network traffic from wherever it is.

### Scenario

An incident response team needs to see what a suspect app on a remote device is sending, to identify command-and-control (C2) communications.

### How MESHadb helps

1. **Join the device** - As above
2. **Start a capture** - `capture start` begins recording to the device's acquisition folder
3. **Route the traffic** - `forward on` sends the device's internet traffic through the analyst
4. **Analyse** - Inspect the pcap with tshark, Wireshark or Suricata

!!! example "Workflow"
    ```text
    meshcli> capture start
    meshcli> forward on
    ... let the device run ...
    meshcli> capture stop
    meshcli> forward off
    ```

See [Exit node and packet capture](../advanced/exit-node-pcap.md).

### Benefits

- **Remote deployment** - No need to travel
- **Real network conditions** - Capture what an app sends from the device on its real network
- **Evidence in one place** - The pcap lands next to the device's acquisition

## Human rights investigations

Securely analyse devices of activists, journalists, and human rights defenders in hostile environments where network traffic is monitored or censored.

### Scenario

A journalist in an authoritarian country suspects their phone has been compromised. They need their device analysed but cannot risk exposing the investigation.

### How MESHadb helps

1. **Encrypted communication** - All forensic data is protected from interception
2. **HTTPS fallback** - Works where UDP and VPN protocols are blocked
3. **Remote analysis** - No need to transport the device across borders
4. **Consent** - The journalist controls every step on their own device

!!! warning "Operational security"
    MESHadb protects the content of the session, not the fact that it happened.

    - Share the address and the PIN over separate channels, at least one of them end-to-end encrypted, such as Signal
    - Relays, and each end of the session, can see IP addresses. MESHadb is not an anonymity tool
    - Run `adbclean` and `adbdisable` after the acquisition, in that order

    See the [Security model](../reference/security.md).

## Censored network operations

Operate in countries with aggressive internet censorship.

### Scenario

A security researcher needs to examine devices on a network where UDP, WireGuard and VPN protocols are actively blocked or degraded.

### How MESHadb helps

1. **HTTPS relay fallback** - When UDP is blocked, the session runs over HTTPS on TCP port 443
2. **Automatic adaptation** - The session moves to a direct path whenever one becomes available
3. **Circumvention** - With `forward on`, the device reaches the internet from the analyst's location

### Benefits

- **Works where plain WireGuard fails** - A blocked UDP path does not end the session
- **Looks like web traffic** - A relayed session is an HTTPS connection to the network in between

### MESHadb provides in all cases

- **Speed** - Start a session in minutes
- **No persistent infrastructure** - Sessions are ephemeral and nothing is stored
- **Secure evidence handling** - Encrypted transfer straight into the analyst's acquisitions folder
- **Minimal disruption** - No need to power down or transport devices

!!! example "Incident response timeline"
    - **T+0 min**: Incident detected
    - **T+5 min**: `meshcli` running, PIN shared
    - **T+10 min**: App installed, device joined, ADB paired
    - **T+15 min**: Acquisition begins
    - **T+60 min**: Evidence collected, device cleaned up and disconnected

---

**Next:** Check [Platform support](platform-support.md) to see which platforms are supported →
