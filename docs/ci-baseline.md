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
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
      - run: mvn --batch-mode --no-transfer-progress verify
```

This workflow is installed at `.github/workflows/ci.yml`.
