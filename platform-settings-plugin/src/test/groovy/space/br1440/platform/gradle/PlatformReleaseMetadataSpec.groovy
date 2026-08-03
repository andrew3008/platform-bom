package space.br1440.platform.gradle

import spock.lang.Specification

/**
 * Структурный контракт сгенерированного PlatformReleaseMetadata.
 */
class PlatformReleaseMetadataSpec extends Specification {

    def 'RELEASE_VERSION is a valid version string'() {
        expect:
        PlatformReleaseMetadata.RELEASE_VERSION ==~ /\d+\.\d+\.\d+.*/
    }

    def 'SPRING_BOOT_VERSION is a valid version string'() {
        expect:
        PlatformReleaseMetadata.SPRING_BOOT_VERSION ==~ /\d+\.\d+\.\d+.*/
    }

    def 'JAVA_VERSION is a positive integer'() {
        expect:
        PlatformReleaseMetadata.JAVA_VERSION ==~ /\d+/
        Integer.parseInt(PlatformReleaseMetadata.JAVA_VERSION) > 0
    }

    def 'CATALOG_GAV is a well-formed Maven GAV pointing at the current release'() {
        given:
        def parts = PlatformReleaseMetadata.CATALOG_GAV.split(':' as Closure)

        expect: 'three colon-separated segments'
        parts.length == 3

        and: 'group matches the platform group id'
        parts[0] == 'space.br1440.platform'

        and: 'artifact name is platform-catalog'
        parts[1] == 'platform-catalog'

        and: 'version segment equals RELEASE_VERSION'
        parts[2] == PlatformReleaseMetadata.RELEASE_VERSION
    }

    def 'PlatformReleaseMetadata cannot be meaningfully instantiated'() {
        when:
        def constructor = PlatformReleaseMetadata.getDeclaredConstructor()
        constructor.accessible = true
        constructor.newInstance()

        then:
        noExceptionThrown()
    }
}
