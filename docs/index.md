---
hide:
  - navigation
  - toc
---

# [ MESHadb ]

MESHadb gives you ADB access to an Android device anywhere in the world, without port forwarding, accounts or a server to run. The phone runs a small app, your computer runs the `meshcli` command-line client, and the two join an end-to-end encrypted, peer-to-peer tunnel using an address and a one-time PIN.

It is built on [MESHnet](https://github.com/BARGHEST-ngo/MESHnet), BARGHEST's library for peer-to-peer WireGuard connections, and is designed primarily for consensual remote forensics of spyware and surveillance.

**Key features:**

- **Peer-to-peer first** - Your computer and the phone connect directly over WireGuard-encrypted UDP, punching through NAT on both sides
- **HTTPS relay fallback** - When UDP is blocked or hole punching fails, traffic falls back to a relay over HTTPS on TCP port 443. The relay only ever sees encrypted packets
- **Nothing to deploy** - No control plane, no accounts. Every session gets new keys and a new address, and nothing persists when it ends

**What you can do with MESHadb:**

- **Remote Android forensics** - Pair with a device's Wireless debugging from anywhere
- **Automated acquisition** - `adbcollect` runs an [AndroidQF](https://github.com/mvt-project/androidqf)/WARD acquisition, ready for [MVT](https://github.com/mvt-project/mvt)
- **Network monitoring** - Route a device's traffic through your machine and save it to a pcap
- **Censorship circumvention** - Give a device on a restricted network a way out through a machine you trust
- **Clean up afterwards** - Clear shell-accessible logs and turn developer mode back off

## Remote ADB, made for mobile forensics

<div class="grid cards" markdown>

- :fontawesome-solid-hexagon-nodes:{ .lg .middle} **Features**

    ---

    What MESHadb does, and what it deliberately does not do

    [:octicons-arrow-right-24: View features](overview/features.md)

- :material-network:{ .lg .middle } **Architecture**

    ---

    How the CLI, the app and the relay fit together

    [:octicons-arrow-right-24: Explore architecture](overview/architecture.md)

- :material-briefcase:{ .lg .middle } **Use cases**

    ---

    See how MESHadb helps investigators in real-world scenarios

    [:octicons-arrow-right-24: Read use cases](overview/use-cases.md)

- :material-devices:{ .lg .middle } **Platform support**

    ---

    Check which platforms are supported and what's coming next

    [:octicons-arrow-right-24: View platforms](overview/platform-support.md)

</div>

## Quick start

<div class="grid cards" markdown>

- :material-rocket-launch:{ .lg .middle } **Getting started guide**

    ---

    From zero to your first acquisition

    [:octicons-arrow-right-24: Start here](setup/index.md)

</div>

**Or jump directly to a step:**

1. **[Start the analyst client](setup/analyst-client.md)** - Run `meshcli` on your computer
2. **[Install the Android app](setup/endpoint-client.md)** - Install MESHadb on the device
3. **[Run your first session](setup/verification.md)** - Join, pair ADB and collect

## Learn more

### Documentation

- **[Architecture](overview/architecture.md)** - How MESHadb works under the hood
- **[User guide](user-guide/user-guide.md)** - Forensic workflows and best practices
- **[Exit node and packet capture](advanced/exit-node-pcap.md)** - Capture a device's network traffic
- **[CLI reference](reference/cli-reference.md)** - Every `meshcli` command
- **[Security model](reference/security.md)** - What MESHadb protects, and what it does not
- **[Troubleshooting](reference/troubleshooting.md)** - Common issues and solutions

### About the project

- **[About BARGHEST](overview/about.md)** - Learn about the team behind MESHadb
- **[Platform support](overview/platform-support.md)** - Current and planned platform support
- **[Get help](overview/about.md#get-help)** - Community, support, and contributing

---
