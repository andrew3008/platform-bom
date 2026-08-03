package space.br1440.platform.gradle

import spock.lang.Requires

class PlatformLibraryPluginFunctionalTest extends PlatformFunctionalSpec {

    def 'C1: java-library is applied'() {
        given:
        writeSettings()
        writeLibraryBuild("""
            tasks.register('verifyJavaLibrary') {
                doLast {
                    println configurations.findByName('api') != null ? 'API_PRESENT' : 'API_ABSENT'
                }
            }
        """)

        when:
        def result = runner('verifyJavaLibrary').build()

        then:
        result.output.contains('API_PRESENT')
    }

    def 'C2: org.springframework.boot is not applied'() {
        given:
        writeSettings()
        writeLibraryBuild()

        when:
        def result = runner('tasks', '--all').build()

        then:
        !result.output.contains('bootJar')
        !result.output.contains('bootRun')
        result.output.contains('jar')
    }

    def 'C3: the Java toolchain matches the version declared in the platform catalog'() {
        given:
        writeSettings()
        writeLibraryBuild("""
            tasks.register('verifyToolchain') {
                doLast {
                    def java = project.extensions
                        .getByType(org.gradle.api.plugins.JavaPluginExtension)
                    println "toolchain.languageVersion=\${java.toolchain.languageVersion.get().asInt()}"
                }
            }
        """)

        when:
        def result = runner('verifyToolchain').build()

        then:
        result.output.contains("toolchain.languageVersion=${EXPECTED_JAVA_VERSION}")
    }

    def 'C4: the platform BOM is present in every version-managed configuration'() {
        given:
        writeSettings()
        writeLibraryBuild("""
            tasks.register('verifyBomCoverage') {
                doLast {
                    ['api', 'implementation', 'compileOnly', 'compileOnlyApi',
                     'annotationProcessor',
                     'testImplementation', 'testCompileOnly', 'testAnnotationProcessor'
                    ].each { name ->
                        def found = configurations.getByName(name).dependencies.find {
                            it.group == 'space.br1440.platform' && it.name == 'platform-bom'
                        }
                        assert found != null : "platform-bom missing from configuration: \${name}"
                        println "bom-in:\${name}"
                    }
                }
            }
        """)

        when:
        def result = runner('verifyBomCoverage').build()

        then:
        ['api', 'implementation', 'compileOnly', 'compileOnlyApi',
         'annotationProcessor',
         'testImplementation', 'testCompileOnly', 'testAnnotationProcessor'].every {
            result.output.contains("bom-in:${it}")
        }
    }

    @Requires({ !Boolean.getBoolean('test.offline') })
    def 'C5: commons-lang3 resolves to the platform catalog pin via platform-bom'() {
        given:
        writeSettings()
        writeLibraryBuild("""
            dependencies {
                api 'org.apache.commons:commons-lang3'
            }
            tasks.register('verifyLang3') {
                doLast {
                    def resolved = configurations.compileClasspath
                        .resolvedConfiguration.resolvedArtifacts
                        .find { it.name == 'commons-lang3' }
                    assert resolved != null : 'commons-lang3 not on compileClasspath'
                    println "resolved-lang3:\${resolved.moduleVersion.id.version}"
                }
            }
        """)

        when:
        def result = runner('verifyLang3').build()

        then:
        result.output.contains("resolved-lang3:${EXPECTED_COMMONS_LANG3_VERSION}")
    }

    def 'C6: the test task uses the JUnit Platform'() {
        given:
        writeSettings()
        writeLibraryBuild("""
            tasks.register('verifyTestFramework') {
                doLast {
                    def framework = (tasks.getByName('test') as Test).testFramework
                    println "testFramework=\${framework.class.name}"
                }
            }
        """)

        when:
        def result = runner('verifyTestFramework').build()

        then:
        result.output.toLowerCase().contains('junitplatform')
    }

    def 'C7: build fails with a diagnostic message when the platform catalog is absent'() {
        given:
        settingsFile << "rootProject.name = 'fixture-no-catalog'\n"
        buildFile << """
            plugins {
                id 'space.br1440.platform.library' version '${RELEASE_VERSION}'
            }
        """.stripIndent()

        when:
        def result = runner('tasks').buildAndFail()

        then:
        result.output.contains('[platform]')
        result.output.contains('settings') || result.output.contains('catalog')
    }

    def 'C8: library build fails when it declares a repositories block'() {
        given:
        writeSettings()
        writeLibraryBuild("repositories { mavenLocal() }")

        when:
        def result = runner('tasks').buildAndFail()

        then:
        result.output.contains('Build was configured to prefer settings repositories')
    }

    def 'C9: convention plugin is compatible with the configuration cache'() {
        given:
        writeSettings()
        writeLibraryBuild()

        when:
        def first = runner('tasks', '--configuration-cache').build()

        then:
        first.output.contains('BUILD SUCCESSFUL')

        when:
        def second = runner('tasks', '--configuration-cache').build()

        then:
        second.output.contains('BUILD SUCCESSFUL')
        second.output.contains('Reusing configuration cache')
    }
}
