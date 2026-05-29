# TODO Debt Register

`scripts/todo-debt-report.sh` is the canonical inventory for TODO/FIXME/HACK debt in this repo.

Run:

```bash
./scripts/todo-debt-report.sh
```

Outputs:

- `target/todo-debt/raw.txt`
- `target/todo-debt/todo-report.tsv`
- `target/todo-debt/summary.txt`

Triage rules:

- Security, authorization, data loss, migration, and release-blocking TODOs are `P0/P1`.
- Imported upstream TODOs stay visible, but they should not block release unless they affect a Sihsalus workflow.
- False positives from translation strings or fixtures should be excluded in the script when confirmed.
- New TODOs need an owner or a tracking issue reference.

Initial focus areas:

- object-level authorization gaps in patient-facing modules
- PostgreSQL migration safety and upgrade fixture coverage
- billing, stock, reporting, and attachment workflow tests
- Sihsalus-specific interoperability placeholders
