# CI Baseline

The intended first CI workflow is a Java 21 Maven verification job:

```yaml
name: CI

on:
  pull_request:
  push:
    branches:
      - main

permissions:
  contents: read

jobs:
  java:
    name: Java baseline
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v6
        with:
          fetch-depth: 0
      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
      - run: git diff --check <base> HEAD
      - run: mvn --batch-mode --no-transfer-progress verify
```

This workflow is installed at `.github/workflows/ci.yml`.

The installed workflow computes `<base>` from the pull request base SHA, the push
`before` SHA, or the root commit fallback. This keeps new branches from being
checked against the repository root and surfacing unrelated historical
whitespace.
