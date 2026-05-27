# Deploy Assets

This directory contains local deployment assets for the backend runtime.

- `compose.yml`: local PostgreSQL plus backend smoke/runtime stack.
- `docker/`: backend image build and entrypoint files.

Run Compose commands from the repository root:

```bash
docker compose -f deploy/compose.yml config
docker compose -f deploy/compose.yml up -d backend
```
