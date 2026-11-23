# CI and release

Every MESHadb release job runs on a freshly provisioned GitHub-hosted runner. This page documents the Android release flow, its guarantees, and its inputs. Exact action SHAs, cache keys, and toolchain pins live in the workflow and script files that enforce them.

## Release workflow

The release path has two entry points:

- Tag pushes matching `v*` for real releases.
- `workflow_dispatch` for maintainers who need to rebuild a specific version string manually.

Pull requests touching `android-client/**` or the workflow itself run the same build in unsigned mode, so reproducibility problems show up before release.

## `android-release.yml`

Path: `.github/workflows/android-release.yml`. Five jobs: `build-release`, `verify-reproducibility`, `sign-release`, `provenance`, and `create-release`.

### Job 1: `build-release`

Runs on `ubuntu-latest` with `android-client` as the working directory:

1. Checks out the repo and installs the Java and Android toolchain pinned in the workflow.
2. Runs `make release-apk`, which downloads the pinned MESHnet AAR, verifies its SHA-256, and builds the unsigned APK.
3. Hashes the unsigned APK and exposes that hash as a job output.
4. Derives a version string from the tag, manual input, or PR number, and uploads the unsigned APK as an artifact.

### Job 2: `verify-reproducibility`

Runs after `build-release`, on a separate `ubuntu-latest` runner. It installs the same pinned toolchain, rebuilds the unsigned APK, and compares its SHA-256 hash to the one exported by `build-release`.

**What this proves:** the unsigned APK is reproducible across two clean runners. If the hashes differ, the release path introduced nondeterminism somewhere in source generation, filesystem ordering, timestamps, or toolchain behaviour.

**Why unsigned?** The unsigned APK is the deterministic artifact. Signing adds a signature block, so the signed APK is expected to differ even when the unsigned payload is reproducible.

### Job 3: `sign-release`

Skipped on pull requests. Runs in the `android-signing` environment, which requires reviewer approval, so signing credentials are only injected here and never in a job that runs build code.

1. Downloads the unsigned APK and re-checks its hash against `build-release`'s output. A mismatch refuses to sign.
2. Decodes the keystore and signs with `apksigner` directly from the runner image, not through `make`.
3. Hashes the signed APK, computes the provenance subject, and uploads the signed APK.
4. Removes the keystore unconditionally.

### Job 4: `provenance`

Runs only on tag push. Calls the reusable workflow:

```yaml
uses: slsa-framework/slsa-github-generator/.github/workflows/generator_generic_slsa3.yml@<pinned-release-ref>
with:
  base64-subjects: ${{ needs.sign-release.outputs.hashes }}
  upload-assets: false
```

This produces SLSA Level 3 provenance over the signed APK. It runs in a separate reusable workflow, which reduces the chance that the build job can tamper with provenance generation. The repo's action-pinning rule intentionally excludes this reusable workflow because SLSA verification expects a release-style ref.

### Job 5: `create-release`

Runs only on tag push, after `sign-release` and `provenance`. Creates the GitHub Release in one step with both the signed APK and the provenance file attached, and `generate_release_notes: true`. Creating it complete in one call keeps it compatible with immutable releases.

### Secrets

Configured as environment secrets on `android-signing`, not as repo-level secrets:

- `KEYSTORE_BASE64` becomes a temporary `mesh-release.jks` file for the signing step.
- `KEYSTORE_PASSWORD` and `KEYSTORE_ALIAS` are passed to `apksigner` as environment variables.

None of the secret values are written intentionally to logs, and GitHub masks them automatically.

## Release script

Path: `scripts/tag-release.sh`. Invoked via `task -t Taskfile.dev.yml tagRelease VERSION=<semver>`.

### Preconditions

The script fails unless all of the following are true:

1. The requested version is strict semver.
2. Required tooling is installed, including `go tool yq`.
3. The working tree is clean.
4. The current branch is `main`.
5. The tag does not already exist locally.
6. If remote checks are enabled, local `main` matches `origin/main` and the tag does not already exist on the remote.

It then runs `task tidy` and fails if that changes any tracked file.

### Version bump

The script intentionally modifies only three files:

- `android-client/android/build.gradle` for the Android `versionCode`.
- `android-client/barghest.version` for the human-readable version fields.
- `android-client/fdroid/com.barghest.mesh.yml` for the new F-Droid build entry and current-version pointers.

### Commit and tag

The script creates one release-preparation commit and one annotated tag, `vX.Y.Z`. It does not push.

---

## Build and release safeguards

- **SLSA L3 provenance** on every Android release, generated on an isolated runner.
- **Reproducibility verification** on every release and on PRs that modify Android build inputs.
- **Isolated signing** in a protected environment, gated on the reproducible unsigned hash.
- **Pinned MESHnet AAR** verified by SHA-256 before every build.
- **Pinned Java and Android toolchains** in both CI and the devcontainer.

---

← [Build orchestration](build-orchestration.md) | [Troubleshooting](troubleshooting.md) →
