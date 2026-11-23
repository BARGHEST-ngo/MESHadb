# Reference

Technical reference documentation, CLI commands, and troubleshooting guides.

## Reference documentation

<div class="grid cards" markdown>

- :material-console:{ .lg .middle } **CLI reference**

    ---

    Every `meshcli` command and its options.

    [:octicons-arrow-right-24: View CLI reference](cli-reference.md)

- :material-shield-lock:{ .lg .middle } **Security model**

    ---

    What MESHadb protects, what it does not, and how to use it safely.

    [:octicons-arrow-right-24: View Security model](security.md)

- :material-help-circle:{ .lg .middle } **Troubleshooting**

    ---

    Common issues, error messages, and solutions.

    [:octicons-arrow-right-24: View Troubleshooting](troubleshooting.md)

- :material-hammer-wrench:{ .lg .middle } **Build and deploy**

    ---

    The container, build system and release pipeline.

    [:octicons-arrow-right-24: View Build and deploy](build-deploy/index.md)

</div>

## Quick reference

### Session (`meshcli>` prompt)

```
pin                      # one-time PIN for a device to join
qr [-o file.png]         # show or save the address QR code
status                   # session, connected device, running capture
adbpair                  # pair and connect to Wireless debugging
adbcollect [flags]       # AndroidQF/WARD acquisition
forward on|off           # route the device's traffic through the analyst
capture start|stop       # record forwarded traffic to a pcap
adbclean                 # clear shell-accessible logs (after the acquisition)
adbdisable               # turn off Developer options and Wireless debugging
disconnect               # end the device's session
exit                     # stop meshcli
```

### Host (`task`)

```bash
task analyst   # build if needed, start the container and meshcli
task shell     # shell in the running container, for adb, tcpdump and MVT
task build     # build the analyst image
task down      # stop the container
```

### Common issues

| Issue | Solution |
|-------|----------|
| Device can't join | PIN expired (10 minutes) or already used: run `pin` again |
| `pin` says too many failed attempts | Restart `meshcli` for a new address |
| `adbpair` fails | Re-read the code and ports from the device's current screen |
| Session dropped | Run `pin` again, rejoin, then `adbpair` again |
| Slow transfers | Session is probably relayed; see [Troubleshooting](troubleshooting.md) |

[:octicons-arrow-right-24: Full troubleshooting guide](troubleshooting.md)

### Network requirements

The analyst and the device only need **outbound** internet access. No inbound ports or port forwarding are needed on either side.

| Port | Protocol | Purpose |
|------|----------|---------|
| 443 | TCP (HTTPS) | Relay connection, and fallback path when UDP is blocked |
| 3478 | UDP | STUN, for discovering public endpoints |
| Ephemeral | UDP | Direct WireGuard path after hole punching |

## Getting help

- **Search the docs** - Use the search bar at the top
- **Check troubleshooting** - [Troubleshooting guide](troubleshooting.md)
- **Report issues** - [GitHub Issues](https://github.com/BARGHEST-ngo/MESHadb/issues)

---

**Next:** Browse the [CLI reference](cli-reference.md) or [Troubleshooting guide](troubleshooting.md) →
