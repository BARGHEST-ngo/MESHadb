# Overview

Learn about MESHadb: what it is, how it works, and why it suits secure remote mobile forensic investigations.

## What you'll learn

This section covers:

- **Core features** and capabilities
- **Architecture** and how the pieces fit together
- **Real-world use cases** and scenarios
- **Platform support** and compatibility
- **About the project** and the team behind it

## Quick Navigation

<div class="grid cards" markdown>

- :material-star-four-points:{ .lg .middle } **Features**

    ---

    End-to-end encryption, peer-to-peer connections with an HTTPS fallback, remote ADB, acquisition and traffic capture.

    [:octicons-arrow-right-24: View features](features.md)

- :material-sitemap:{ .lg .middle } **Architecture**

    ---

    Two components, the `meshcli` analyst client and the Android app, plus a relay that only carries encrypted packets.

    [:octicons-arrow-right-24: View Architecture](architecture.md)

- :material-lightbulb-on:{ .lg .middle } **Use cases**

    ---

    Real-world scenarios where MESHadb provides critical capabilities for forensic investigations.

    [:octicons-arrow-right-24: View Use cases](use-cases.md)

- :material-devices:{ .lg .middle } **Platform support**

    ---

    Which platforms are currently supported and what's planned for future releases.

    [:octicons-arrow-right-24: View platforms](platform-support.md)

- :material-information:{ .lg .middle } **About**

    ---

    Learn about BARGHEST, the team behind MESHadb, licensing, and how to get help.

    [:octicons-arrow-right-24: About MESHadb](about.md)

</div>

## Why MESHadb?

MESHadb is designed for **digital forensics and mobile device analysis** in challenging environments.

### Problem

- **Spyware targeting** - Activists and journalists face sophisticated surveillance
- **Remote access** - Physical access to the device is often impossible or unsafe
- **ADB is local** - Wireless debugging only works when the computer and the phone share a network
- **Censorship** - Restricted networks often block UDP and VPN protocols

### Solution

- **No infrastructure** - No control plane, accounts or server to run
- **Peer-to-peer** - Direct WireGuard connections with NAT traversal on both sides
- **Works where UDP is blocked** - Falls back to an HTTPS relay on TCP port 443
- **Ephemeral** - New keys and a new address every session, single-use PINs
- **Consent built in** - The device owner installs the app, enters the PIN and enables Wireless debugging themselves

## Getting started

Ready to try MESHadb? Head to the [Getting started guide](../setup/index.md).

Already familiar with MESHadb? Jump to:

- [User guide](../user-guide/index.md) - Forensic workflows and usage
- [Advanced](../advanced/index.md) - Network capture and exit node
- [Reference](../reference/index.md) - CLI reference and troubleshooting

---

**Next:** Explore [Features](features.md) to see what MESHadb can do →
