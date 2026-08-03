package space.br1440.platform.gradle

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.initialization.resolve.RepositoriesMode

class PlatformSettingsPlugin implements Plugin<Settings> {

    private static final Map<String, String> PLATFORM_REPOSITORIES = [
            platformMaven   : 'https://n.1440.space/repository/platform-maven/',
            platformMavenDev: 'https://n.1440.space/repository/platform-maven-dev/',
            gradlePluginsProxy: 'https://n.1440.space/repository/plugins.gradle.org-proxy/',
            mavenCentralProxy : 'https://n.1440.space/repository/repo1.maven.org-proxy',
    ].asImmutable()

    private static final String CATALOG_ACCESSOR = 'platform'

    @Override
    void apply(Settings settings) {
        configurePluginManagement(settings)
        configureDependencyResolution(settings)
    }

    private static void configurePluginManagement(Settings settings) {
        settings.pluginManagement {
            repositories {
                PLATFORM_REPOSITORIES.each { repositoryName, repositoryUrl ->
                    maven {
                        name = "${repositoryName}Plugins"
                        url = repositoryUrl
                    }
                }
            }

            plugins {
                id('org.springframework.boot')
                        .version(PlatformReleaseMetadata.SPRING_BOOT_VERSION)

                id('space.br1440.platform.spring-boot-application')
                        .version(PlatformReleaseMetadata.RELEASE_VERSION)

                id('space.br1440.platform.library')
                        .version(PlatformReleaseMetadata.RELEASE_VERSION)
            }
        }
    }

    private static void configureDependencyResolution(Settings settings) {
        settings.dependencyResolutionManagement {
            repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

            repositories {
                PLATFORM_REPOSITORIES.each { repositoryName, repositoryUrl ->
                    maven {
                        name = repositoryName
                        url = repositoryUrl
                    }
                }
            }

            versionCatalogs {
                "${CATALOG_ACCESSOR}" {
                    from(PlatformReleaseMetadata.CATALOG_GAV)
                }
            }
        }
    }
}
