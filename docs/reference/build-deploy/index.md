# Build and deploy

This section documents how MESHadb is containerised, built and released. It's written for analysts, contributors, and anyone auditing the supply chain. For end-user setup, see the [Setup guide](../../setup/index.md).

## What's in this section

- **[Runtime stack](runtime-stack.md)**: `compose.yml` and the analyst `Dockerfile`.
- **[Build orchestration](build-orchestration.md)**: the Task-based build system (`Taskfile.yml`, `Taskfile.dev.yml`), the analyst build script, the Android build, and the VS Code devcontainer.
- **[CI and release](ci-and-release.md)**: the Android release workflow and `scripts/tag-release.sh`.
- **[Build troubleshooting](troubleshooting.md)**: build and container-layer issues. For session, ADB and app problems, see [Troubleshooting](../troubleshooting.md).

## Key design decisions

### 1. No server-side components

MESHadb has no control plane, web UI or provisioning service. The only runtime component you run is the analyst container. Peers find each other through public relays, and all session state lives in memory in `meshcli`.

### 2. Docker Compose for the runtime, go-task for orchestration

`compose.yml` defines a single `analyst` service. [go-task](https://taskfile.dev) wraps the few commands users need (`task analyst`, `task shell`, `task down`) and the developer tasks in `Taskfile.dev.yml`.

### 3. Multi-stage Dockerfile with pinned base images

The analyst `Dockerfile` builds `meshcli` in a Go stage and copies only the binary into a slim Debian runtime. Both base images are pinned by digest.

### 4. Ephemeral, unprivileged analyst container

`task analyst` runs the container with `docker compose run --rm`, so it is deleted when `meshcli` exits. `meshcli` creates new keys on every start and saves no state. The entrypoint drops to the unprivileged `mesh` user, and the only added capability is `NET_RAW`, for `tcpdump`. The only persistent path is the acquisitions bind mount.

### 5. Reproducible Android releases

The Android release pipeline rebuilds the unsigned APK on a second, isolated runner and fails the release if the SHA-256 hashes differ. Signing runs in a separate job with its own protected environment, and SLSA Level 3 provenance is generated via the `slsa-github-generator` reusable workflow. The MESHnet Android library is downloaded from a pinned MESHnet release and verified against a SHA-256 in the Makefile.

Reproducibility is a security benefit on its own. F-Droid has argued that VPN apps require an unusually high trust baseline that reproducible builds help establish. See F-Droid's [VPN trust requires Free Software](https://f-droid.org/en/2023/03/08/vpn-trust-requires-free-software.html) post.

### 6. Supply-chain hardening

- GitHub Actions are pinned to commit SHAs via [frizbee](https://github.com/stacklok/frizbee), refreshed on demand with `task -t Taskfile.dev.yml pinGithubActions`.
- The MESHnet version is pinned in two places that must stay in sync: `analyst/go.mod` (Go module) and `android-client/Makefile` (`MESHNET_VERSION` and `MESHNET_SHA256` for the AAR).

### 7. Config via `.env`, never committed

The only runtime setting is `ACQUISITION_DIR`, read from `.env` at the repo root. It defaults to `$HOME/mesh-acquisitions`.

## Assumptions

- You have Docker Engine with the Compose V2 plugin (`docker compose`, not `docker-compose`).
- You have the [go-task](https://taskfile.dev) CLI installed. Each task is a thin wrapper, so plain `docker compose` works too.
- You're on Linux or macOS. Windows via WSL may work but is untested.
- For Android work: Go matching `go.work`, plus the Java/Android toolchain declared in `.devcontainer/Dockerfile` and `.github/workflows/android-release.yml`. Or just use the [devcontainer](build-orchestration.md#development-container).

---

**Next:** [Runtime stack](runtime-stack.md)
