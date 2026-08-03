package space.br1440.platform.gradle

class PlatformSettingsPluginFunctionalTest extends PlatformFunctionalSpec {

    // ------------------------------------------------------------------
    // Успешный сценарий
    // ------------------------------------------------------------------

    def 'plugin applies cleanly on a bare Java project'() {
        given:
        writeSettings()
        buildFile << "plugins { id 'java' }\n"

        when:
        def result = runner('tasks').build()

        then:
        result.output.contains('BUILD SUCCESSFUL')
    }

    // ------------------------------------------------------------------
    // Политика репозиториев
    // ------------------------------------------------------------------

    def 'build fails when a service declares a #description block in build.gradle'() {
        given:
        writeSettings()
        buildFile << """
            plugins { id 'java' }
            repositories { ${repositoryDeclaration} }
        """.stripIndent()

        when:
        def result = runner('tasks').buildAndFail()

        then:
        result.output.contains('Build was configured to prefer settings repositories')

        where:
        description    | repositoryDeclaration
        'mavenCentral' | 'mavenCentral()'
        'mavenLocal'   | 'mavenLocal()'
    }

    // ------------------------------------------------------------------
    // Регистрация каталога версий
    // ------------------------------------------------------------------

    def 'platform catalog is registered under the platform accessor with the BOM alias'() {
        given:
        writeSettings()
        buildFile << """
            plugins { id 'java' }
            tasks.register('verifyCatalog') {
                doLast {
                    def catalogs = project.extensions
                        .getByType(org.gradle.api.artifacts.VersionCatalogsExtension)
                    def catalog = catalogs.find('platform')
                    assert catalog.isPresent() : "'platform' catalog not registered by settings plugin"
                    def bom = catalog.get().findLibrary('platform-bom')
                    assert bom.isPresent() : "'platform-bom' alias missing from platform catalog"
                    println "platform-bom GAV: \${bom.get().get()}"
                }
            }
        """.stripIndent()

        when:
        def result = runner('verifyCatalog').build()

        then: 'the alias resolves to the release version, proving the published catalog was imported'
        result.output.contains("platform-bom GAV: space.br1440.platform:platform-bom:${RELEASE_VERSION}")
        result.output.contains('BUILD SUCCESSFUL')
    }

    def 'platform catalog exposes the documented consumer aliases'() {
        given:
        writeSettings()
        buildFile << """
            plugins { id 'java' }
            tasks.register('verifyCatalogAliases') {
                doLast {
                    def catalog = project.extensions
                        .getByType(org.gradle.api.artifacts.VersionCatalogsExtension)
                        .find('platform').get()
                    // Spring Boot starters are deliberately not catalog aliases: services
                    // declare them by coordinate and platform-bom supplies the version.
                    ['commons-lang3',
                     'jetbrains-annotations'].each { alias ->
                        assert catalog.findLibrary(alias).isPresent() : "missing alias: \${alias}"
                    }
                    println 'All expected catalog aliases present'
                }
            }
        """.stripIndent()

        when:
        def result = runner('verifyCatalogAliases').build()

        then:
        result.output.contains('All expected catalog aliases present')
    }

    def 'catalog versions come from the platform catalog TOML'() {
        given:
        writeSettings()
        buildFile << """
            plugins { id 'java' }
            tasks.register('printCatalogVersions') {
                doLast {
                    def catalog = project.extensions
                        .getByType(org.gradle.api.artifacts.VersionCatalogsExtension)
                        .find('platform').get()
                    println "spring-boot=\${catalog.findVersion('spring-boot').get().requiredVersion}"
                    println "java=\${catalog.findVersion('java').get().requiredVersion}"
                }
            }
        """.stripIndent()

        when:
        def result = runner('printCatalogVersions').build()

        then: 'the value matches the constant generated from the same TOML'
        result.output.contains("spring-boot=${PlatformReleaseMetadata.SPRING_BOOT_VERSION}")
        result.output.contains("java=${PlatformReleaseMetadata.JAVA_VERSION}")
    }

    // ------------------------------------------------------------------
    // Configuration cache
    // ------------------------------------------------------------------

    def 'plugin is compatible with the Gradle configuration cache'() {
        given:
        writeSettings()
        buildFile << "plugins { id 'java' }\n"

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

    // ------------------------------------------------------------------
    // Fixture wiring
    // ------------------------------------------------------------------

    def 'fixture repository is wired, otherwise every other spec here is meaningless'() {
        expect:
        isFixtureRepositoryAvailable()
        RELEASE_VERSION != null
    }
}
