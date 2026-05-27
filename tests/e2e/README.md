# End-to-End Tests

Use this directory for black-box end-to-end tests that exercise Sihsalus across modules or through deployed application boundaries.

Module-specific unit and integration tests should stay in each module's `src/test` tree. End-to-end tests in this directory should keep generated files out of source control and write reports or temporary output to build directories such as `target/e2e`.

Suggested layout:

- `scripts/`: executable helpers for running or preparing E2E scenarios.
- `fixtures/`: small, deterministic input data used by E2E scenarios.
- `reports/`: local output location for generated E2E reports; committed files should be limited to placeholders or documentation.
