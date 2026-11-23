# Runtime stack

This page documents `compose.yml` and the analyst `Dockerfile`. The devcontainer (`.devcontainer/Dockerfile`) is covered separately in [Build orchestration](build-orchestration.md#development-container) because it is a build-time environment, not a runtime component.

## Compose file

Path: `compose.yml` (repo root).

The Compose file defines a single service, `analyst`:

```yaml
services:
  analyst:
    build:
      context: .
      dockerfile: analyst/Dockerfile
    cap_add:
      - NET_RAW
    volumes:
      - ${ACQUISITION_DIR:-${HOME}/mesh-acquisitions}:/home/mesh/acquisitions:z
    stdin_open: true
    tty: true
```

### Service: `analyst`

Built locally from `analyst/Dockerfile` with the repo root as build context.

**Started with `run`, not `up`:** `task analyst` runs `docker compose run --rm --build --name mesh-analyst analyst`. `meshcli` runs in the foreground with an interactive terminal (`stdin_open`, `tty`), and the container is removed when it exits. The fixed name `mesh-analyst` lets `task shell` attach a second shell.

**Capabilities:** only `NET_RAW`, so `tcpdump` can capture forwarded traffic. No `NET_ADMIN`, no `/dev/net/tun` and no `sysctls`: MESHnet runs entirely in user space and forwards traffic by opening connections itself, so the container needs no tunnel device, IP forwarding or NAT rules.

**Networking:** the default Compose bridge network. The container needs outbound internet access only; no ports are published.

**Volumes:** a single bind mount for acquisitions, from `${ACQUISITION_DIR:-$HOME/mesh-acquisitions}` on the host to `/home/mesh/acquisitions`. The `:z` suffix relabels it for SELinux hosts. This is the only path that survives the container.

**Environment:** none. `meshcli` takes no configuration.

---

## `analyst/Dockerfile`

Path: `analyst/Dockerfile`. Two stages, both pinned by digest.

### Stage 1: Go build

```dockerfile
FROM golang:<pinned>-alpine AS build
WORKDIR /usr/src/app
ENV GOWORK=off
COPY analyst/go.mod analyst/go.sum ./analyst/
RUN cd analyst && go mod download
COPY analyst ./analyst
RUN ./analyst/src/build.sh
```

- `GOWORK=off` builds the analyst module on its own, without the repo's `go.work`.
- `go mod download` runs before the source copy so the dependency layer is cached across code changes.
- `analyst/src/build.sh` is the single source of truth for the build command. It is called both here and from `task -t Taskfile.dev.yml buildNode`. See [Build orchestration](build-orchestration.md#analyst-build-script).

### Stage 2: Runtime

`debian:trixie-slim`, with:

- `adb` - Android Debug Bridge, used by `meshcli` and from `task shell`
- `tcpdump` - packet capture, given the `cap_net_raw` file capability so the `mesh` user can capture
- `suricata` - IDS, for offline pcap analysis
- `iproute2`, `iputils-ping` - network diagnostics
- `util-linux` - `setpriv`, used by the entrypoint to drop privileges
- **MVT** - installed with pip into a virtualenv at `/opt/mvt`, which is on `PATH`. `python3-venv` is purged afterwards

It creates the `mesh` user (uid/gid 1000) and copies in:

- `/usr/src/app/analyst/mesh` from the build stage to `/usr/bin/meshcli`
- `analyst/entrypoint.sh` to `/usr/bin/entrypoint.sh`

The working directory is `/home/mesh/acquisitions`, so acquisitions land in the bind mount by default. `CMD` is `meshcli`.

### Entrypoint

`analyst/entrypoint.sh` (bash, `set -eo pipefail`) runs as root only long enough to:

1. Make sure `/home/mesh/acquisitions` is writable by `mesh`, taking ownership of the bind mount if needed (bind mounts arrive root-owned)
2. Drop to `mesh` with `setpriv`, keeping no ambient capabilities, and `exec` the command

`tcpdump` gets `NET_RAW` from its file capability, not from the process, so nothing privileged stays in the running session.

---

## Dependencies and assumptions

- **Docker Engine with Compose V2 support**. The stack expects the modern `docker compose` plugin rather than legacy `docker-compose`.
- **Outbound internet access** from the container, including HTTPS (TCP 443) to the relays.
- **Acquisition directory ownership**: the host directory should be owned by uid/gid 1000, or writable so the entrypoint can take ownership.

---

← [Overview](index.md) | [Build orchestration](build-orchestration.md) →
