# MESH Documentation

This directory contains the documentation for MESHadb, built with [MkDocs](https://www.mkdocs.org/) and the [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/) theme.

## Overview

MESHadb gives remote ADB access to Android devices over a peer-to-peer, end-to-end encrypted tunnel, built on [MESHnet](https://github.com/BARGHEST-ngo/MESHnet). It is designed primarily for consensual remote forensics. This documentation covers setup, forensic workflows, network capture and reference material.

## Documentation Structure

- **Overview** - Introduction, architecture, features, platform support, and use cases
- **Setup** - Starting the analyst client, installing the Android app, and a first session
- **User Guide** - Usage patterns and workflows
- **Advanced** - Exit node and packet capture
- **Reference** - CLI reference, security model, troubleshooting, and build and release

## Local Development

### Prerequisites

- Python 3.x
- pipx

### Installation

```bash
pipx install mkdocs
pipx inject mkdocs mkdocs-material
```

### Running Locally

```bash
mkdocs serve
```

The documentation will be available at `http://localhost:8000`

### Building

To build the static site:

```bash
mkdocs build
```

Output will be generated in the `site/` directory.

## Live Documentation

The documentation is automatically deployed to [https://docs.meshforensics.org/](https://docs.meshforensics.org/) when changes under `docs/` are pushed to the main branch. CI runs `markdownlint-cli2` and `mkdocs build --strict` first, so check both locally before opening a pull request.

## Contributing

To contribute to the documentation:

1. Make changes to the markdown files in the `docs/` directory
2. Test locally with `mkdocs serve`
3. Submit a pull request
