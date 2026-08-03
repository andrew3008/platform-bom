package space.br1440.platform.gradle

import spock.lang.Requires

@Requires({ !Boolean.getBoolean('test.offline') })
class PlatformSpringBootApplicationPluginFunctionalTest extends PlatformFunctionalSpec {

    def 'C1+C2: java and spring-boot plugins are applied'() {
        given:
        writeSettings()
        writeAppBuild()

        when:
        def result = runner('tasks', '--all').build()

        then: 'java plugin tasks are present'
        result.output.contains('compileJava')
        result.output.contains('processResources')

        and: 'spring-boot plugin tasks are present'
        result.output.contains('bootJar')
        result.output.contains('bootRun')
    }

    def 'C3: io.spring.dependency-management is not applied'() {
        given:
        writeSettings()
        writeAppBuild("""
            tasks.register('checkDepMgmt') {
                doLast {
                    def ext = project.extensions.findByName('dependencyManagement')
                    println ext == null ? 'DEP_MGMT_ABSENT' : 'DEP_MGMT_PRESENT'
                }
            }
        """)

        when:
        def result = runner('checkDepMgmt').build()

        then:
        result.output.contains('DEP_MGMT_ABSENT')
        !result.output.contains('DEP_MGMT_PRESENT')
    }

    def 'C4: the Java toolchain matches the version declared in the platform catalog'() {
        given:
        writeSettings()
        writeAppBuild("""
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

        then: 'no literal here — the expectation comes from the same TOML the plugin reads'
        result.output.contains("toolchain.languageVersion=${EXPECTED_JAVA_VERSION}")
    }

    def 'C5: bootJar and bootRun tasks are registered'() {
        given:
        writeSettings()
        writeAppBuild()

        when:
        def result = runner('tasks', '--all').build()

        then:
        result.output.contains('bootJar')
        result.output.contains('bootRun')
    }

    def 'C6: the platform BOM is present in every version-managed configuration'() {
        given:
        writeSettings()
        writeAppBuild("""
            tasks.register('verifyBomCoverage') {
                doLast {
                    ['implementation', 'compileOnly', 'annotationProcessor',
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

        then: 'injecting into implementation alone would leave processors and tests unmanaged'
        ['implementation', 'compileOnly', 'annotationProcessor',
         'testImplementation', 'testCompileOnly', 'testAnnotationProcessor'].every {
            result.output.contains("bom-in:${it}")
        }
    }

    def 'C7: spring-boot-starter resolves to the platform Boot version'() {
        given:
        writeSettings()
        writeAppBuild("""
            dependencies {
                implementation 'org.springframework.boot:spring-boot-starter'
            }
            tasks.register('verifyBootVersion') {
                doLast {
                    def resolved = configurations.compileClasspath
                        .resolvedConfiguration.resolvedArtifacts
                        .find { it.name == 'spring-boot-starter' }
                    assert resolved != null :
                        'spring-boot-starter not on compileClasspath — BOM import failed'
                    println "resolved-boot:\${resolved.moduleVersion.id.version}"
                }
            }
        """)

        when:
        def result = runner('verifyBootVersion').build()

        then: 'the version traces back to the catalog TOML through the published BOM'
        result.output.contains("resolved-boot:${EXPECTED_SPRING_BOOT_VERSION}")
    }

    def 'C8: the test task uses the JUnit Platform'() {
        given:
        writeSettings()
        writeAppBuild("""
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
        result.output.toLowerCase().contains('testframework=') &&
                result.output.toLowerCase().contains('junitplatform')
    }

    def 'C9: build fails with a diagnostic message when the platform catalog is absent'() {
        given: 'the settings plugin is intentionally not applied, so no catalog is registered'
        settingsFile << "rootProject.name = 'fixture-no-catalog'\n"
        buildFile << """
            plugins {
                id 'space.br1440.platform.spring-boot-application' version '${RELEASE_VERSION}'
            }
        """.stripIndent()

        when:
        def result = runner('tasks').buildAndFail()

        then: 'the message names the root cause rather than failing on a missing alias'
        result.output.contains('[platform]')
        result.output.contains('settings') || result.output.contains('catalog')
    }

    def 'C10: service build fails when it declares a repositories block'() {
        given:
        writeSettings()
        writeAppBuild("repositories { mavenLocal() }")

        when:
        def result = runner('tasks').buildAndFail()

        then:
        result.output.contains('Build was configured to prefer settings repositories')
    }

    def 'C11: convention plugin is compatible with the configuration cache'() {
        given:
        writeSettings()
        writeAppBuild()

        when: 'first run stores the entry'
        def first = runner('tasks', '--configuration-cache').build()

        then:
        first.output.contains('BUILD SUCCESSFUL')

        when: 'second run must reuse it'
        def second = runner('tasks', '--configuration-cache').build()

        then:
        second.output.contains('BUILD SUCCESSFUL')
        second.output.contains('Reusing configuration cache')
    }
}
