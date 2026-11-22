# Dependency updates

The app is shipped from `main`, so dependency PRs are treated as release work,
not as background noise.

## Automatic groups

- GitHub Actions updates arrive as one PR.
- Gradle **minor and patch** updates arrive as one PR.
- Security fixes still arrive through Dependabot security updates.

## Manual migration only

These move together in one deliberate branch and are never auto-merged:

- Gradle wrapper major
- Android Gradle Plugin major
- Kotlin and Kotlin Compose/serialization plugin major
- Compose BOM major

A toolchain migration needs:

1. a clean `chore/toolchain-*` branch;
2. Gradle, AGP, Kotlin, Compose compiler, and Compose BOM updated together;
3. `make check`, lint, content audit, and a signed release build;
4. phone and tablet smoke tests;
5. a new `versionCode`, not a rebuild of the submitted release.

## Release window rule

While a Play release is under review, app dependency PRs stay unmerged. CI-only
action updates may be prepared separately because they do not change the AAB.

## Pull request order

1. Keep `v1.0.4` and versionCode 14 frozen.
2. After review, merge one grouped GitHub Actions PR.
3. Then the grouped Gradle minor/patch PR, with backup and release-build tests.
4. Then a planned toolchain migration, if needed.
5. Close individual stale Dependabot PRs once the grouped PRs are merged.

## Current state

Dependabot opened nine separate PRs before this policy:

- Actions: checkout 4 -> 7, setup-java 4 -> 6, setup-android 3 -> 4,
  gradle/actions 4 -> 6.
- Gradle minor: appcompat 1.7.0 -> 1.8.0, serialization 1.7.3 -> 1.13.0.
- Toolchain major: Gradle 8.11.1 -> 9.9.1, AGP 8.7.3 -> 9.5.2,
  Kotlin 2.2.21 -> 2.4.21.

The grouped Actions and Gradle minor/patch PRs supersede the individual ones.
