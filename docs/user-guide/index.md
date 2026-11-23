# User guide

Learn how to use MESHadb for forensic investigations and day-to-day tasks.

## Overview

This section covers:

- **Forensic workflows** - How to perform remote device analysis
- **Acquisitions and evidence** - Where acquisitions are stored, and how to analyse them
- **Common tasks** - Day-to-day operations and best practices

## Documentation

<div class="grid cards" markdown>

- :material-book-open-variant:{ .lg .middle } **User guide**

    ---

    Workflows, tools and best practices for MESHadb investigations.

    [:octicons-arrow-right-24: View User guide](user-guide.md)

- :material-folder-lock:{ .lg .middle } **Acquisitions and evidence**

    ---

    Where acquisitions land, how to get them off the container, and analysing them with MVT.

    [:octicons-arrow-right-24: View Acquisitions](acquisitions.md)

</div>

## Quick reference

At the `meshcli>` prompt:

```
# Create a one-time PIN for the device to join
pin

# Show the address QR code again, or save it as a PNG
qr
qr -o /home/mesh/acquisitions/session-qr.png

# Pair and connect to the device's Wireless debugging (interactive)
adbpair

# Run an AndroidQF/WARD acquisition into the acquisitions folder
adbcollect
adbcollect --output /home/mesh/acquisitions/case-2026-08-27

# Route the device's internet traffic through the analyst, and record it
capture start
forward on
capture stop
forward off

# Show the session
status

# After the acquisition: clean up, turn off developer mode, end the session
adbclean
adbdisable
disconnect
```

In a second terminal, from `task shell`:

```bash
# Talk to the device directly
adb devices
adb shell

# Analyse an acquisition with MVT
mvt-android download-iocs
mvt-android check-androidqf /home/mesh/acquisitions/case-2026-08-27
```

## Getting help

- **Troubleshooting:** [Troubleshooting guide](../reference/troubleshooting.md)
- **CLI reference:** [CLI reference](../reference/cli-reference.md)
- **Issues:** [GitHub Issues](https://github.com/BARGHEST-ngo/MESHadb/issues)

---

**Next:** Read the [User guide](user-guide.md) for detailed workflows →
