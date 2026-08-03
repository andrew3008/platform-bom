# ADR-0002. Convention plugins are consumed from Nexus, not from a composite build

## Status

Accepted.

## Context

`settings.gradle` declared `includeBuild 'platform-gradle-conventions'`, pointing at a directory that does not exist inside this project. `platform-bom/build.gradle` applied `platform.java-platform-publish` from that missing build, and the Gradle wrapper JAR was absent while CI invoked `./gradlew`. The build could not be evaluated at all.

The real convention plugins live in a separate repository and are already published as `space.br1440.platform.conventions:platform-gradle-conventions`, with plugin markers for `platform.java-base`, `platform.java-library`, `platform.java-library-publish`, `platform.java-platform-publish` and `platform.testing`. The production `platform-bom` repository consumes them from Nexus.

## Decision

Resolve `platform.java-platform-publish` from Nexus via `pluginManagement`, pinned to an explicit version. Do not use `includeBuild`.

Ownership stays split: this repository owns release-pack concerns (BOM, catalog, settings bootstrap, application convention), while `platform-gradle-conventions` owns generic Java library, testing and publishing conventions.

## Consequences

- The build no longer depends on a sibling directory being checked out next to it, and matches how the production `platform-bom` repository already works.
- `platformConventionsVersion` in `settings.gradle` is a bootstrap pin that cannot live in the TOML: convention plugins must resolve before the catalog is materialised. Its scope is this repository only — consuming services never apply `platform.java-platform-publish`, so it does not propagate.
- Changing a convention plugin now requires publishing it first. This is a deliberate trade: reproducibility over local iteration speed.
- The Gradle wrapper is committed (8.14, aligned with the production `platform-bom` and `Platform_Traces`).
