# Deploy Assets

This directory contains local deployment assets for the backend runtime.

- `compose.yml`: local PostgreSQL plus backend smoke/runtime stack.
- `docker/`: backend image build and entrypoint files.

Run Compose commands from the repository root:

```bash
docker compose -f deploy/compose.yml config
docker compose -f deploy/compose.yml up -d backend
```

## Image Build

The default backend Dockerfile is compatible with classic Docker Compose builds.

Local build:

```bash
docker compose -f deploy/compose.yml build backend
```

CI and GHCR publishing use `docker/Dockerfile.buildkit`, which keeps Maven's local
repository in a BuildKit cache mount and exports that cache through GitHub Actions.
