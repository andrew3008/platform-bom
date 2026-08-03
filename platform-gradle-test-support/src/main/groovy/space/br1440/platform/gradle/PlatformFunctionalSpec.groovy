package space.br1440.platform.gradle

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

abstract class PlatformFunctionalSpec extends Specification {

    protected static final String FIXTURE_REPO = System.getProperty('platform.fixture.repo')
    protected static final String RELEASE_VERSION = System.getProperty('platform.release.version')
    protected static final String LOCAL_INIT_SCRIPT = System.getProperty('platform.local.init.script')

    protected static final String EXPECTED_SPRING_BOOT_VERSION = System.getProperty('platform.spring.boot.version')
    protected static final String EXPECTED_JAVA_VERSION = System.getProperty('platform.java.version')
    protected static final String EXPECTED_COMMONS_LANG3_VERSION = System.getProperty('platform.commons.lang3.version')

    @TempDir
    protected Path testProjectDir

    protected File getSettingsFile() {
        testProjectDir.resolve('settings.gradle').toFile()
    }

    protected File getBuildFile() {
        testProjectDir.resolve('build.gradle').toFile()
    }

    private File writeInitScript() {
        def repoUri = new File(FIXTURE_REPO).toURI().toString()
        def initScript = testProjectDir.resolve('platform-fixture.init.gradle').toFile()

        if (initScript.exists()) {
            return initScript
        }

        initScript << """
            beforeSettings { settings ->
                settings.pluginManagement {
                    repositories {
                        maven { url = uri('${repoUri}') }
                    }
                    plugins {
                        id('space.br1440.platform.settings').version('${RELEASE_VERSION}')
                    }
                }
                settings.dependencyResolutionManagement {
                    repositories {
                        maven { url = uri('${repoUri}') }
                    }
                }
            }
        """.stripIndent()

        initScript
    }

    /** Fixture settings applying only the platform settings plugin. */
    protected void writeSettings(String projectName = 'fixture-project') {
        settingsFile << """
            plugins {
                id 'space.br1440.platform.settings'
            }
            rootProject.name = '${projectName}'
        """.stripIndent()
    }

    protected void writeBuild(String pluginId, String extra = '') {
        buildFile << """
            plugins {
                id '${pluginId}'
            }
            ${extra}
        """.stripIndent()
    }

    protected void writeAppBuild(String extra = '') {
        writeBuild('space.br1440.platform.spring-boot-application', extra)
    }

    protected void writeLibraryBuild(String extra = '') {
        writeBuild('space.br1440.platform.library', extra)
    }

    protected GradleRunner runner(String... args) {
        def initScript = writeInitScript()
        def arguments = args as List
        if (LOCAL_INIT_SCRIPT) {
            arguments += ['--init-script', LOCAL_INIT_SCRIPT]
        }
        arguments += ['--init-script', initScript.absolutePath, '--stacktrace']

        GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments(arguments as String[])
                .forwardOutput()
    }

    protected static boolean isFixtureRepositoryAvailable() {
        FIXTURE_REPO != null && new File(FIXTURE_REPO).isDirectory()
    }
}
