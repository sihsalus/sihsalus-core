# Technical Debt Phase 1 Baseline

Phase 1 freezes a clean base for the technical-debt cleanup work. This phase is intentionally about control and traceability, not large refactors.

## Baseline

- Base branch: `main`
- Base commit: `5fe4e5c2` (`Merge pull request #42 from sihsalus/quality-simple-cleanups-fixed`)
- Working branch: `technical-debt-phase-1-baseline`
- Remote: `origin`
- Main divergence at setup time: `0 0` for `main...origin/main`

## Goals

1. Start all future cleanup work from an updated `main`.
2. Keep the worktree clean before each CodeQL or modernization batch.
3. Separate formatting-only changes from behavior changes.
4. Keep each commit focused on one rule, module, or warning family.
5. Avoid suppressing findings with `@SuppressWarnings` unless there is a documented compatibility reason.
6. Avoid artificial no-op helpers that exist only to silence static analysis.

## Branch Rules

- Use `technical-debt-phase-1-baseline` for this baseline documentation.
- Create separate implementation branches from updated `main` for each cleanup batch.
- Use a dedicated formatting branch if Spotless or line-ending cleanup changes many unrelated files.
- Do not commit unrelated local changes together with CodeQL fixes.
- Before every commit, review:

```bash
git status --short --branch
git diff --cached --stat
git diff --cached --check
```

## Validation Standard

For documentation-only phase-1 changes:

```bash
git diff --check
```

For code cleanup branches:

```bash
git diff --check
mvn -pl <affected-module> -am -DskipTests compile
mvn -pl <affected-module> spotless:check
```

If `spotless:check` fails because of unrelated modules, isolate the affected module and document the blocker in the PR.

## Cleanup Policy

- Fix reliability warnings before maintainability notes when both are available.
- Prefer removing unused private parameters over suppressing warnings.
- For public APIs, do not remove parameters without checking all implementations and callers.
- For framework callbacks, keep required signatures and document real framework ownership.
- For parser fixes, handle invalid input explicitly and keep user-facing behavior stable.
- For resource cleanup, prefer `try-with-resources` and clear ownership.

## Initial Backlog Order

1. Resource leaks.
2. Null dereferences.
3. Numeric parsing without `NumberFormatException` handling.
4. Unused containers and locals.
5. Useless private/protected parameters.
6. Static nested classes.
7. Boxed locals that cannot be null.
8. String cleanup warnings.
9. Javadoc cleanup.
10. Public API cleanup and compatibility review.

## Completion Criteria

Phase 1 is complete when:

- `main` is confirmed current.
- A clean baseline branch exists.
- The baseline document is committed and pushed.
- The worktree is clean after push.
- Future cleanup work has a documented branching and validation workflow.
