# ADR-0003. Repository resolution is fail-closed

## Status

Accepted.

## Context

`PlatformSettingsPlugin` added the internal Nexus repository only when a `NEXUS_URL` environment variable or `nexusUrl` property was present, and always appended `gradlePluginPortal()` and `mavenCentral()`. With the variable unset — the default on a developer machine — every service silently resolved plugins and dependencies from the public internet while the build kept succeeding.

The same plugin applied `mavenContent { releasesOnly() }` to the Nexus repository, which makes the `platform-maven-dev` snapshot line unusable.

Publishing credentials were read eagerly at configuration time via `credentials { username = provider.getOrNull() }`, which writes the secret into the on-disk configuration cache entry.

## Decision

Declare the four internal Nexus proxies as the only repositories, both in this build's `settings.gradle` and in `PlatformSettingsPlugin`:

- `platform-maven`, `platform-maven-dev`, `plugins.gradle.org-proxy`, `repo1.maven.org-proxy`

`gradlePluginPortal()`, `mavenCentral()` and `mavenLocal()` are not declared. `releasesOnly()` is not applied.

Publishing uses `credentials(PasswordCredentials)`, which resolves `nexusSnapshotUsername` / `nexusReleaseUsername` and the matching passwords at execution time. CI maps them from the corporate `PROD_NEXUS_*` variables through `ORG_GRADLE_PROJECT_*`.

## Consequences

- Losing Nexus access produces a resolution failure instead of an unvetted artifact. This is the intended behaviour.
- The public repositories remain reachable, but only through the audited Nexus proxies.
- Secrets no longer reach the configuration cache entry. This is verified with a sentinel value rather than asserted.
- The hardcoded repository list makes the settings plugin untestable against a local fixture repository without external injection; functional tests supply the fixture repository through a TestKit init script rather than through a production override property.
