# ADR-0004. Ownership split between the release pack and the conventions repository

## Status

Accepted.

## Context

Two repositories publish Gradle plugins under the `space.br1440.platform` umbrella:

- `platform-gradle-conventions` — `platform.java-base`, `platform.java-library`, `platform.java-library-publish`, `platform.java-platform-publish`, `platform.testing`;
- this release pack — `space.br1440.platform.settings`, `space.br1440.platform.spring-boot-application`, and `space.br1440.platform.library`.

Without an explicit boundary this looks like duplicated ownership of "convention plugins", and the natural reflex is to consolidate everything into one repository. In particular `platform.java-library` (conventions) and `space.br1440.platform.library` (this repository) must not be confused.

## Decision

Keep the split, along this line:

- **`platform-gradle-conventions` owns build-time conventions for any Java project**: encoding, Javadoc, test defaults, library and platform publishing mechanics (`platform.java-library`, `platform.java-library-publish`, …). These are applied per module by a project that already knows what it is building.
- **This repository owns the platform release contract**: which dependency versions constitute a platform release (`platform-bom`), how consumers discover them (`platform-catalog`), how a project bootstraps (`space.br1440.platform.settings`), what a platform Spring Boot service is (`space.br1440.platform.spring-boot-application`), and what a platform pure Java library is (`space.br1440.platform.library` — toolchain pin from the catalog + `platform-bom` injection, without Spring Boot).

`space.br1440.platform.library` does **not** replace `platform.java-library`. The former is the release-train contract; the latter is generic build hygiene. A library module may apply both when both concerns are required.

The distinction is release cadence and blast radius. A conventions change affects how a module compiles; a release-pack change affects which versions every consumer resolves. They version independently, and merging them would force one cadence onto both.

## Consequences

- This repository depends on `platform-gradle-conventions` (see ADR-0002), never the reverse. The dependency direction is acyclic.
- `platform.java-platform-publish` supplies publishing for `platform-bom`. No conventions plugin covers `groovy-gradle-plugin` marker publication or `version-catalog` publication, so the other four modules take it from `platform-nexus-publishing`, a precompiled script plugin in this repository's `buildSrc`. It is build-internal and never published, which keeps the release pack at five artifacts.
- A future `platform.gradle-plugin-publish` convention in `platform-gradle-conventions` would let that local convention go away. It is not introduced speculatively.
- The settings plugin version literal in each service's `settings.gradle` remains the single bootstrap pin. Propagating it across services is a repository-maintenance concern (centralised MR automation), deliberately out of scope here.
