# Security Baseline

Sihsalus Core handles clinical data and must be treated as sensitive infrastructure from the start.

## Rules

- No secrets in git.
- No patient data in tests, logs, screenshots, or fixtures unless synthetic.
- Authentication and session behavior must remain compatible with the frontend until a migration plan exists.
- Production cookies must be secure, HTTP-only where applicable, and scoped intentionally.
- CORS must be explicit.
- TLS termination must be documented.
- Database credentials must come from runtime secrets.
- Administrative bootstrap accounts must be rotated or disabled after setup.

## Required Before Production

- dependency vulnerability scan
- container scan
- SBOM
- backup and restore test
- audit logging review
- role/privilege review
- log redaction review
- disaster recovery notes

## Initial Tooling Direction

- Maven Enforcer for Java/Maven constraints
- SpotBugs with security plugin when Java modules are imported
- Dependabot or equivalent for dependency updates
- GitHub branch protection before production branches are used

