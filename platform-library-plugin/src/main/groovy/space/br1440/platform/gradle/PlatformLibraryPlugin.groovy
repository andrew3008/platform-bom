package space.br1440.platform.gradle

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion

class PlatformLibraryPlugin implements Plugin<Project> {

    static final String CATALOG_NAME = 'platform'
    static final String BOM_ALIAS = 'platform-bom'
    static final String JAVA_VERSION_ALIAS = 'java'

    private static final List<String> PLATFORM_BOM_CONFIGURATIONS = [
            'api',
            'implementation',
            'compileOnly',
            'compileOnlyApi',
            'annotationProcessor',
            'testImplementation',
            'testCompileOnly',
            'testAnnotationProcessor',
    ].asImmutable()

    @Override
    void apply(Project project) {
        VersionCatalog catalog = resolvePlatformCatalog(project)

        applyPlugins(project)
        configureJavaToolchain(project, catalog)
        importPlatformBom(project, catalog)
        configureTestTask(project)
    }

    private static void applyPlugins(Project project) {
        project.pluginManager.apply('java-library')
    }

    private static void configureJavaToolchain(Project project, VersionCatalog catalog) {
        String javaVersion = catalog.findVersion(JAVA_VERSION_ALIAS)
                .orElseThrow {
                    new GradleException(
                            "[platform] Version '${JAVA_VERSION_ALIAS}' not found in catalog " +
                            "'${CATALOG_NAME}'. Ensure the platform-catalog TOML defines: " +
                            "[versions] java = \"21\"")
                }
                .requiredVersion

        project.extensions.configure(JavaPluginExtension) { java ->
            java.toolchain {
                languageVersion = JavaLanguageVersion.of(Integer.parseInt(javaVersion))
            }
        }
    }

    private static void importPlatformBom(Project project, VersionCatalog catalog) {
        Provider<MinimalExternalModuleDependency> bomProvider = catalog.findLibrary(BOM_ALIAS)
                .orElseThrow {
                    new GradleException(
                            "[platform] Library alias '${BOM_ALIAS}' not found in catalog " +
                            "'${CATALOG_NAME}'. Ensure the platform-catalog TOML defines: " +
                            "platform-bom = { group = 'space.br1440.platform', " +
                            "name = 'platform-bom', version.ref = 'platform-release' }")
                }

        def platformDependency = bomProvider.map { dependency ->
            project.dependencies.platform(dependency)
        }

        PLATFORM_BOM_CONFIGURATIONS.each { configurationName ->
            project.dependencies.addProvider(configurationName, platformDependency)
        }
    }

    private static VersionCatalog resolvePlatformCatalog(Project project) {
        def catalogs = project.extensions.findByType(VersionCatalogsExtension)
        if (catalogs == null) {
            throw new GradleException(
                    '[platform] VersionCatalogsExtension not found. ' +
                    "Did you apply 'space.br1440.platform.settings' in settings.gradle?")
        }

        def platformCatalog = catalogs.find(CATALOG_NAME)
        if (!platformCatalog.isPresent()) {
            throw new GradleException(
                    "[platform] Version Catalog '${CATALOG_NAME}' not found. " +
                    'Expected the settings plugin to register it.')
        }

        platformCatalog.get()
    }

    private static void configureTestTask(Project project) {
        project.tasks.withType(Test).configureEach { test ->
            test.useJUnitPlatform()
        }
    }
}
