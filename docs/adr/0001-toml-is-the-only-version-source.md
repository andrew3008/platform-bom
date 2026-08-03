# ADR-0001. The platform catalog TOML is the only source of dependency versions

## Status

Accepted.

## Context

The release pack previously carried five competing version sources, which had already diverged:

| Source | Spring Boot | commons-lang3 | jetbrains-annotations | release |
|---|---|---|---|---|
| `platform-catalog.versions.toml` | 3.5.16 | 3.20.0 | 26.1.0 | 1.0.0 |
| root `gradle.properties` | 3.5.6 | — | — | 1.0.0 |
| `platform-bom/gradle.properties` | — | 3.19.0 | 26.0.2-1 | — |
| `platform-release.toml` | 3.5.6 | 3.19.0 | 26.0.2-1 | 1.1.0 |
| `PlatformReleaseMetadata.groovy` | 3.5.6 | — | — | 1.0.0 |

Divergence was policed by a Spock "four-way consistency gate" that walked the filesystem to locate the sources and parsed the TOML with the regex `^[a-z][a-z0-9-]*\s*=\s*".+"`. That is a lock file implemented as a test: it detects drift after the fact instead of preventing it, and its regex parser silently mismatches on reordered keys or unquoted values.

## Decision

`platform-catalog/gradle/platform-catalog.versions.toml` is the only place a version is declared. Every other source is deleted, not reconciled.

The TOML is consumed three times: registered as the `platform` catalog in `settings.gradle`, published as the `platform-catalog` artifact, and used to generate `PlatformReleaseMetadata` via `:platform-settings-plugin:generatePlatformReleaseMetadata`.

Build scripts read the catalog through `VersionCatalogsExtension`, not through type-safe accessors. The catalog accessor would be named `platform` and would collide with the `DependencyHandler.platform(...)` notation used inside `dependencies { }` blocks; relying on Groovy dispatch order to disambiguate is exactly the implicit behaviour this repository's policy forbids.

## Consequences

- Version drift is no longer expressible, so the consistency gate and its TOML regex parser are deleted as tautologies. `PlatformReleaseMetadataSpec` keeps only structural assertions.
- Bumping a platform release is a one-line edit.
- `PlatformReleaseMetadata` becomes a generated file. Its generator task is declared inline in `platform-settings-plugin/build.gradle` and is deliberately **not** `@CacheableTask`: a task type declared in a build script has its implementation identity tied to the script classloader, so cached entries would be invalidated by any edit to that script, and the task writes a single small file. Configuration cache reuse across runs was verified.
- A missing or renamed catalog key now fails the build at configuration time with an explicit message naming the expected `[versions]` or `[libraries]` entry.
