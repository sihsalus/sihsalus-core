# Technical Debt Phase 2 Quality Tooling

Phase 2 makes local and CI validation predictable. The goal is to catch whitespace, formatting, and compile failures before a CodeQL cleanup branch is pushed, without forcing unrelated tools into every change.

## Scope

- Improve the local quality script.
- Make CI whitespace checks compare the correct commit range.
- Keep SpotBugs optional until the project has an agreed SpotBugs baseline.
- Document the validation commands expected for technical-debt cleanup branches.

## Local Quality Script

Use `scripts/quality-doctor.sh` before committing code changes.

Default behavior:

```bash
./scripts/quality-doctor.sh --modules modules/patientflags
```

The default sequence is:

1. `git diff --check`
2. `mvn ... spotless:check` for the selected modules only
3. `mvn ... -am -DskipTests compile`
4. tests are skipped unless `--tests` is passed

Useful options:

```bash
./scripts/quality-doctor.sh --modules core/api,modules/reporting
./scripts/quality-doctor.sh --modules modules/stockmanagement --tests
./scripts/quality-doctor.sh --modules modules/patientflags --skip-spotless
./scripts/quality-doctor.sh --modules core/api --spotbugs
```

The script also accepts Maven artifact IDs for compatibility with older notes.

SpotBugs is intentionally opt-in. Use the fully-qualified plugin through the script instead of running `mvn spotbugs:*`, because the repository does not expose a short `spotbugs` Maven prefix consistently.

## Formatting Fixes

Use `scripts/quality-fix.sh` only for formatting branches or isolated modules. It runs Spotless only on the selected modules:

```bash
./scripts/quality-fix.sh --modules modules/patientflags
```

Do not mix broad Spotless output with behavioral CodeQL fixes.

## CI Whitespace Check

The CI workflow checks changed-file whitespace with the correct base:

- pull requests: PR base SHA to `HEAD`
- pushes: `github.event.before` to `HEAD`
- first commit fallback: root commit to `HEAD`

This avoids checking the entire repository history for whitespace when a new branch is pushed.

## Expected Branch Validation

For code cleanup branches:

```bash
git status --short --branch
git diff --check
./scripts/quality-doctor.sh --modules <affected-module>
```

For documentation-only branches:

```bash
git diff --check
```

## Known Constraints

- Some modules may still have unrelated Spotless drift. If a module-level `spotless:check` fails on files outside the intended change, isolate the failure and document it in the PR.
- Full `mvn verify` is still the CI baseline, but local cleanup branches should prefer module-scoped validation for speed.
- CodeQL findings should be fixed directly. Do not add `@SuppressWarnings` or no-op helpers as a substitute for correcting the code.

## Completion Criteria

Phase 2 is complete when:

- `scripts/quality-doctor.sh` starts with `git diff --check`.
- `scripts/quality-doctor.sh` runs Spotless and compile by default.
- tests are available through `--tests`.
- SpotBugs is optional through `--spotbugs`.
- CI whitespace checks use a correct base range for PRs and pushes.
- The phase-2 workflow is documented and linked from `docs/README.md`.
