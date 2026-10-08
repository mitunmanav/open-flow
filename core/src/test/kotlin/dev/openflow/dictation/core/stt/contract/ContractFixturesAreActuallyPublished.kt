package dev.openflow.dictation.core.stt.contract

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Proves the published `testFixtures` artifact is not empty.
 *
 * ### Why this exists
 *
 * ADR-0006's whole claim is that what providers assert against is a *published*
 * artifact. On this toolchain that claim is false by default: AGP creates a real
 * `testFixtures` variant for a library module, but the Kotlin Android plugin
 * creates no Kotlin compilation for it, so `.kt` sources in
 * `src/testFixtures/kotlin` compile into nothing and the variant's `classes.jar`
 * ships empty — while `:core:assembleDebug` reports success and
 * `compileDebugTestFixturesJavaWithJavac` reports NO-SOURCE.
 *
 * A provider module would then compile against nothing, silently, and report
 * green. This test is the only thing in the build that notices, which is why it
 * reads the published directory rather than trusting the wiring that filled it.
 *
 * The directory path arrives as a system property from
 * `publishDebugContractTestFixtures` in `core/build.gradle.kts`, and the task
 * depends on it. If the mechanism is ever removed, this test fails on the missing
 * property rather than passing because it found nothing to look at.
 */
class ContractFixturesAreActuallyPublished {

    @Test
    fun thePublishedFixturesContainTheSuite() {
        val property = "openflow.contractTestFixturesDir"
        val path = System.getProperty(property)
        assertTrue(
            "system property '$property' was not set, so nothing wired the " +
                "published-fixtures directory into this test. Either " +
                "publishDebugContractTestFixtures stopped running before " +
                "testDebugUnitTest, or this test is no longer checking anything.",
            path != null,
        )

        val published = File(path!!)
        assertTrue(
            "the published testFixtures directory $published does not exist. The " +
                "variant would be empty and a provider module would compile against " +
                "nothing while reporting success.",
            published.isDirectory,
        )

        val classFiles = published.walkTopDown().filter { it.extension == "class" }.toList()
        assertTrue(
            "the published testFixtures directory holds no classes, so the Contract " +
                "Test suite does not reach a provider module. Found ${published.listFiles()?.toList()}",
            classFiles.isNotEmpty(),
        )

        // Both suite classes, because a provider needs the always-on half and a
        // provider that can drive an utterance needs the other one. Checking both
        // catches a partial wiring that published one file's classes.
        val names = classFiles.map { it.name }.toSet()
        listOf(
            "SpeechProviderContract.class",
            "TranscriptionContract.class",
        ).forEach { expected ->
            assertTrue(
                "$expected is missing from the published fixtures (found $names). A " +
                    "file that compiles in core but is not published is worse than " +
                    "one that does not exist, because a provider author will believe " +
                    "the suite covers what it does not cover.",
                expected in names,
            )
        }
    }

    @Test
    fun nothingOutsideTheContractPackageIsPublished() {
        // The publication includes one package. A fixture file written outside it
        // would compile and run in core but never reach a provider, so this asserts
        // the boundary rather than leaving it to be discovered.
        val published = File(System.getProperty("openflow.contractTestFixturesDir")!!)
        val stray = published.walkTopDown()
            .filter { it.extension == "class" }
            .filterNot { it.relativeTo(published).path.startsWith("dev/openflow/dictation/core/stt/contract/") }
            .toList()

        assertTrue(
            "classes outside dev.openflow.dictation/core/stt/contract are published " +
                "but core's own unit tests are not, which means the publication filter " +
                "and the source layout have drifted. Found ${stray.map { it.relativeTo(published).path }}",
            stray.isEmpty(),
        )
    }
}