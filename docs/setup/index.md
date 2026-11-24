# Getting started with MESHadb

This guide walks you through your first MESHadb session: connecting your analyst workstation to an Android device and collecting an acquisition.

There is no server or control plane to deploy. You need:

- [Docker](https://docs.docker.com/get-docker/) and [Task](https://taskfile.dev/installation/) on your computer
- An Android device with Wireless debugging (Android 11 or later)
- A secure channel to the device owner, such as Signal, to send them the session address and PIN

## Setup overview

1. **[Start the analyst client](analyst-client.md)** - Build and run `meshcli` on your workstation
2. **[Install the Android app](endpoint-client.md)** - Install MESHadb on the device
3. **[Run your first session](verification.md)** - Join the device, pair ADB, collect and clean up

## Quick navigation

<div class="grid cards" markdown>

- :material-laptop:{ .lg .middle } **Analyst client**

    ---

    Build and start `meshcli` on your Linux or macOS workstation

    [:octicons-arrow-right-24: Start the analyst client](analyst-client.md)

- :material-cellphone:{ .lg .middle } **Android app**

    ---

    Install MESHadb on the device you will examine

    [:octicons-arrow-right-24: Install the Android app](endpoint-client.md)

- :material-check-circle:{ .lg .middle } **First session**

    ---

    Join, pair ADB, run your first acquisition and clean up

    [:octicons-arrow-right-24: Run your first session](verification.md)

</div>

## Need help?

If you encounter issues during setup:

- Check the [Troubleshooting guide](../reference/troubleshooting.md)
- Review the [Architecture documentation](../overview/architecture.md) to understand how the pieces work together
- Report issues on [GitHub](https://github.com/BARGHEST-ngo/MESHadb/issues)
