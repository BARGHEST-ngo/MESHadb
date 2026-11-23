# Build orchestration

MESHadb uses [go-task](https://taskfile.dev) for build orchestration and task running. Two Taskfiles split responsibilities:

- `Taskfile.yml` is user-facing.
- `Taskfile.dev.yml` is developer-facing. It auto-loads `.env`.

Both run silently by default.

## `Taskfile.yml`: user-facing tasks

Path: `Taskfile.yml`. Default invocation target.

| Task | What it does |
|---|---|
| `build` | `docker compose build` |
| `analyst` | Alias of `node`. `docker compose run --rm --build --name mesh-analyst analyst`: builds if needed and runs `meshcli` in the foreground |
| `shell` | `docker exec -it --user mesh mesh-analyst bash`: a second shell in the running container |
| `down` | `docker compose down` |

---

## `Taskfile.dev.yml`: developer-facing tasks

Path: `Taskfile.dev.yml`. Invoked via `task -t Taskfile.dev.yml <task>`. Holds every task that a contributor runs and an end user should not.

| Task | What it does |
|---|---|
| `tidy` | `go mod tidy`. Kept separate because it mutates `go.sum` and affects reproducibility. |
| `docs` | `mkdocs build`. |
| `serveDocs` | `mkdocs serve` with live reload on `:8000`. |
| `buildNode` | Alias `buildAnalyst`. Runs `analyst/src/build.sh`. Produces `analyst/mesh`. |
| `buildAndroid` | `make -C android-client apk`. |
| `buildAndroidRelease` | `make -C android-client release-apk`. Unsigned. |
| `buildAndroidSigned` | `make -C android-client release-apk-signed`. Requires `JKS_PATH`, `JKS_PASSWORD`, `JKS_ALIAS` env vars. |
| `clean` | `rm -f analyst/mesh` and `make -C android-client clean`. |
| `tagRelease` | Calls `scripts/tag-release.sh $VERSION`. See [CI and release](ci-and-release.md#release-script). |
| `fdroidInit` | Initialise a local F-Droid repo for reproducibility testing. |
| `fdroidBuild` | `cd fdroid && fdroid build -v -l com.barghest.mesh`. |
| `pinGithubActions` | `go tool github.com/stacklok/frizbee actions .github/workflows`. |

---

## Analyst build script

Path: `analyst/src/build.sh`. Called by the Dockerfile (stage 1) and by `task -t Taskfile.dev.yml buildNode`. Single source of truth for the `meshcli` binary build.

### What it does

1. Resolves the repo root from the script's own location.
2. Runs, from `analyst/`:

    ```bash
    go build -trimpath -o "$ROOT_DIR/analyst/mesh" .
    ```

The binary is named `mesh` in the build output and installed as `/usr/bin/meshcli` in the image.

### Why a separate script rather than inline in the Dockerfile?

- `buildNode` runs it for local development without Docker, so contributors iterating on CLI changes rebuild in seconds.
- Build flags live in one place, so the image and local builds can't drift.

---

## Android build

Path: `android-client/Makefile`. Entry points: `apk` (debug), `release-apk` (unsigned release), `release-apk-signed` and `release` (signed AAB).

The transport comes from the MESHnet Android library (`meshnet.aar`, Peer role only), downloaded from the [MESHnet releases](https://github.com/BARGHEST-ngo/MESHnet/releases):

```make
MESHNET_VERSION := <pinned>
MESHNET_SHA256 := <sha256 of the AAR>
MESHNET_AAR := android/libs/meshnet.aar
```

The AAR is fetched once to `android/libs/meshnet.aar` and verified with `shasum -a 256 -c` before Gradle runs. `make clean` keeps it.

!!! warning "Keep MESHnet versions in sync"
    `MESHNET_VERSION` in `android-client/Makefile` must match the `github.com/BARGHEST-ngo/MESHnet` version in `analyst/go.mod`, so both ends speak the same protocol. Bumping it means updating `MESHNET_SHA256` too.

---

## Development container

Path: `.devcontainer/Dockerfile`, `.devcontainer/devcontainer.json`. Defines a development environment compatible with VS Code's Dev Containers and GitHub Codespaces.

### What the image installs

The image is built from a devcontainers Debian base image. It installs:

- Microsoft OpenJDK, Go, Node.js and pnpm.
- The Android command-line tools and the SDK components needed for Android builds.
- Task CLI.

Java and the Android command-line tools are checksum-verified in the Dockerfile. Go is version-pinned via the `GO_VERSION` build arg. Android SDK licences are auto-accepted with `yes | sdkmanager --licenses`.

### `devcontainer.json` features

- Docker-outside-of-Docker so `docker compose` inside the devcontainer drives the host Docker daemon.
- MkDocs tooling so `task docs` and `task serveDocs` work without extra setup.
- Gradle on `PATH` for Android builds.
- Python plus `fdroidserver` for local F-Droid reproducibility work.

### Mounts and environment

- Binds `~/.android` from the host to `/root/.android` to persist the Android debug keystore across rebuilds.

---

## Dependencies and assumptions

- **`go` on PATH** for `task tidy` and `buildNode`, at the version in `go.work`.
- **Network access** on the first Android build, to download the MESHnet AAR and Gradle dependencies.
- **F-Droid work** requires `fdroidserver`, which requires Python, Gradle, a JDK and the Android SDK. The devcontainer has all of this.
- **Signed releases** require `JKS_PATH`, `JKS_PASSWORD`, and `JKS_ALIAS` environment variables and an existing `.jks` keystore. `buildAndroidSigned` fails if these are missing.

---

← [Runtime stack](runtime-stack.md) | [CI and release](ci-and-release.md) →
