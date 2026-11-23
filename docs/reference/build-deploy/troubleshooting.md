# Build troubleshooting

Build and container-specific issues. For session, ADB and app problems, see [Troubleshooting](../troubleshooting.md).

## Analyst container

### `docker compose build` fails pulling a base image

**Symptoms:**

- `ERROR: failed to pull image ...: manifest unknown` or similar during `task build` or `task analyst`.

**Causes:**

- A pinned base image digest was removed from Docker Hub.
- Docker Hub is rate-limiting.

**Solutions:**

1. **Confirm connectivity**

    ```bash
    docker pull <image-from-the-error>
    ```

2. **If rate-limited**: authenticate with `docker login`. Logged-in pulls have a higher rate limit than anonymous ones.
3. **If the pinned image is actually gone**: update `analyst/Dockerfile` in a PR, and review the devcontainer and CI pins at the same time.

### `go mod download` fails in the build stage

**Symptoms:**

- First-stage build fails with `cannot find module` or a checksum mismatch.

**Cause:**

- The build context is wrong. `analyst/Dockerfile` expects the repo root as context (`context: .` in `compose.yml`), so it can copy `analyst/`. `docker build -f analyst/Dockerfile analyst/` would not find it.

**Solution:**

Always build via `task build` or `task analyst`.

### `task analyst` says the container name is already in use

**Symptoms:**

- `Conflict. The container name "/mesh-analyst" is already in use`.

**Cause:**

- A previous `meshcli` session is still running, for example in another terminal, or a container was left behind.

**Solutions:**

```bash
docker ps -a --filter name=mesh-analyst
task down
docker rm -f mesh-analyst   # if it is still listed
```

### `task shell` says no such container

**Cause:**

- `task shell` attaches to the running `mesh-analyst` container. It only works while `task analyst` is running in another terminal.

### Acquisitions are not writable

**Symptoms:**

- `warning: cannot chown /home/mesh/acquisitions; acquisitions may not be writable` on start, or `adbcollect` fails to create its folder.

**Causes:**

- The host directory is owned by another user and the entrypoint cannot take ownership, for example on rootless Docker or a network filesystem.
- SELinux denies access. `compose.yml` uses `:z` to relabel the mount, which needs the host directory to allow relabelling.

**Solutions:**

```bash
ACQ="${ACQUISITION_DIR:-$HOME/mesh-acquisitions}"
mkdir -p "$ACQ"
sudo chown 1000:1000 "$ACQ"
```

### `capture start` fails with a tcpdump permission error

**Cause:**

- The container did not get `NET_RAW`, for example when started without `compose.yml`, or the runtime strips file capabilities.

**Solution:**

Start the analyst with `task analyst`, which applies `cap_add: [NET_RAW]` from `compose.yml`.

---

## Task (go-task) issues

### `task: command not found`

**Solution:**

Install go-task. See [taskfile.dev/installation](https://taskfile.dev/installation). On Ubuntu:

```bash
sudo snap install task --classic
```

### `task tidy` modifies files unexpectedly

**Symptoms:**

- Fresh checkout followed by `task tidy` modifies `go.sum`.

**Causes:**

- Genuine dependency drift.
- Your Go version differs from the one in `go.work` and `go.mod`.

**Solutions:**

1. **Check your Go version**

    ```bash
    go env GOVERSION GOTOOLCHAIN
    grep -E '^(go|toolchain)' go.work go.mod analyst/go.mod
    ```

2. If the Go versions don't match, switch to the repo's declared version. Otherwise CI and your local tidy will continually diverge.

### `Taskfile.dev.yml` task not found

**Symptoms:**

- `task buildNode` says "task not found".

**Cause:**

- Developer tasks live in `Taskfile.dev.yml`, not the default `Taskfile.yml`.

**Solution:**

Use the `-t` flag:

```bash
task -t Taskfile.dev.yml buildNode
```

---

## Android build

### MESHnet AAR checksum fails

**Symptoms:**

- `make` fails at `shasum -a 256 -c` while downloading `meshnet.aar`.

**Causes:**

- `MESHNET_VERSION` was bumped in `android-client/Makefile` without updating `MESHNET_SHA256`.
- The download was interrupted or altered.

**Solutions:**

1. Delete the partial file and retry:

    ```bash
    rm -f android-client/android/libs/meshnet.aar*
    make -C android-client apk
    ```

2. If it still fails, compare the checksum with the one published on the [MESHnet release](https://github.com/BARGHEST-ngo/MESHnet/releases). Never update `MESHNET_SHA256` to match a file you haven't verified independently.

### The app can't connect to an analyst built from the same branch

**Cause:**

- `MESHNET_VERSION` in `android-client/Makefile` and the MESHnet version in `analyst/go.mod` differ, and the wire protocol changed between them.

**Solution:**

Keep both on the same MESHnet release. A stale `android/libs/meshnet.aar` survives `make clean`; delete it after changing the version.

---

## Devcontainer issues

### Devcontainer build fails on Java install

**Symptoms:**

- `error: unsupported architecture: 'riscv64'` or similar during the JDK stage.

**Cause:**

- The Dockerfile only knows about `amd64` and `arm64` (the two JDK binaries Microsoft publishes). Other host architectures fail.

**Solutions:**

1. Use a `linux/amd64` or `linux/arm64` host. On Apple Silicon, VS Code's devcontainer extension builds an `arm64` variant automatically.
2. On a less common host, you need a different JDK source. Modify the Dockerfile, or don't use the devcontainer.

---

## Release script failures

### `working tree dirty after 'task tidy'`

**Symptoms:**

- The script aborts at the reproducibility check.

**Cause:**

- `go.mod` or `go.sum` is out of date.

**Solution:**

The script prints `git status --short`. Commit the tidy changes in a separate PR first, then rerun the script.

### `go tool yq is not available or not v4`

**Cause:**

- yq is a Go tool declared in `go.mod` (see the `tool` directive). On a fresh clone it hasn't been built yet.

**Solution:**

```bash
go mod download
go tool yq --version
```

---

## CI workflow failures

### `pin-github-actions.yml` fails with "Some github actions versions need pinning"

**Cause:**

- A PR added or modified a workflow and included a non-SHA action reference.

**Solution:**

Run `pinGithubActions` locally and commit the result:

```bash
task -t Taskfile.dev.yml pinGithubActions
git add .github/workflows
git commit -m "ci: pin github actions"
```

### `verify-reproducibility` fails

**Cause:**

- The two unsigned APK builds differ. The job prints a `diffoscope` command to compare them.

**Solution:**

Build the unsigned APK twice locally (`make -C android-client release-apk`, keeping a copy of each), then run `diffoscope` on the two files to find the nondeterministic input, such as a timestamp, file ordering, or an unpinned dependency.

---

## Getting help

If you can't resolve your issue:

1. **Session, ADB and app issues**: see [Troubleshooting](../troubleshooting.md).
2. **File an issue** with the failing command and its output: [github.com/BARGHEST-ngo/MESHadb/issues](https://github.com/BARGHEST-ngo/MESHadb/issues).
3. **Supply-chain concerns** (vulnerability, key compromise, SLSA failure): see [`SECURITY.md`](https://github.com/BARGHEST-ngo/MESHadb/blob/main/SECURITY.md) for private disclosure.

---

← [CI and release](ci-and-release.md)
