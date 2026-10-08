import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "dev.openflow.dictation.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // ADR-0006 puts the Contract Test suite for every SpeechProvider in `core`, so
    // provider modules depend on it. This is a build-configuration decision, not a
    // source-layout one: `testFixtures` is its own source set with its own
    // configurations, and enabling it changes what `core` publishes to consumers.
    testFixtures {
        enable = true
    }

    sourceSets {
        // `src/testFixtures/kotlin` is compiled as part of `test` as well as
        // published as fixtures, because the Kotlin Android plugin does not create
        // a compilation for AGP's `testFixtures` source set at all. See the note
        // above `publishContractTestFixtures` below, which is the long version;
        // the short version is that a suite nobody ever executes is not a
        // published artifact, it is a file.
        //
        // So the fixture sources go through `compileDebugUnitTestKotlin` -- which
        // means core's own unit tests can and do exercise them -- and the classes
        // are then published into the `testFixtures` variant's output directory.
        getByName("test") { java.srcDir("src/testFixtures/kotlin") }
        getByName("testFixtures") { java.srcDir("src/testFixtures/kotlin") }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // `api`, not `implementation`: `Flow<SpeechEvent>` is the shape of core's public
    // contract (ADR-0001), so a consumer reading a provider's events must be able to
    // name Flow. ADR-0005 permits `api` only for re-exported contract types — this is
    // the one place it is used today.
    api(libs.kotlinx.coroutines.core)

    // ADR-0006: the Contract Test suite lives in core's fixtures, so what providers
    // assert against is a published artifact, not an internal.
    testFixturesApi(libs.kotlinx.coroutines.core)

    // `testFixturesApi`, not `testFixturesImplementation`: the published artifact
    // is a set of abstract JUnit4 base classes, so JUnit is part of that
    // artifact's public surface. Left as `implementation`, a provider module that
    // forgot to declare its own JUnit dependency would fail to compile against
    // the suite for a reason that has nothing to do with the provider.
    testFixturesApi(libs.junit)

    testImplementation(libs.junit)
}

/**
 * Puts the Contract Test suite into the published `testFixtures` artifact, which
 * is the entire point of ADR-0006.
 *
 * ### Why this exists at all
 *
 * AGP 8.13 gives a library module a real `testFixtures` variant. The Kotlin
 * Android plugin 2.0.21 does not create a Kotlin compilation for it:
 * `:core:tasks --all` lists `compileDebugTestFixturesJavaWithJavac` and no
 * `compileDebugTestFixturesKotlin`, and javac ignores `.kt` files. So with only
 * `testFixtures { enable = true }` the suite compiles into nothing, the variant's
 * `classes.jar` is empty, `:core:assembleDebug` stays green, and every task in
 * the graph reports either NO-SOURCE or UP-TO-DATE.
 *
 * That is this repository's most-recorded failure — a green check that ran zero
 * tests — reached from the build side instead of the test side, and the worst
 * version of it, because the artifact is published and a provider module would
 * compile against an empty one and report success.
 *
 * The two rejected alternatives are why this is worth the mechanism. A separate
 * `provider-contract-tests` module is what ADR-0005's no-empty-modules rule and
 * ADR-0006's "no provider-api module" decision both forbid before a second real
 * provider exists. Hand-registering a `KotlinCompile` fails on KGP 2.0's
 * constructor-service model: the task needs a `KotlinJvmCompilerOptions`
 * instance injected, and the plugin's only implementation of that interface is
 * `internal`.
 *
 * ### How it works
 *
 * The fixture sources are compiled by `compileDebugUnitTestKotlin` (see the
 * `sourceSets` block above), which is why core's own unit tests can subclass and
 * run the suite — that is what makes "the suite executes" checkable rather than
 * asserted. Their classes are then synced into the directory AGP's own javac
 * task for the `testFixtures` variant writes to, and AGP packages that directory
 * without ever knowing the classes are Kotlin's. The javac task then finds no
 * `.java` sources and leaves them alone.
 *
 * ### The one thing to know when adding a fixture
 *
 * Everything published lives in `dev.openflow.dictation.core.stt.contract`. A
 * fixture file written outside that package would compile and run in core but
 * would not reach a provider module. `ContractSuiteSelfTest` asserts the
 * published set is exactly that package, so that drift fails a test instead of
 * being discovered by a contributor whose provider cannot see the suite.
 */
/**
 * Everything published in the `testFixtures` artifact lives in this package.
 * See the note above for why that is a constraint worth a test.
 */
val contractTestPackageGlob = "dev/openflow/dictation/core/stt/contract/**"

val publishDebugContractTestFixtures =
    registerContractTestFixturePublication("Debug")
registerContractTestFixturePublication("Release")

fun registerContractTestFixturePublication(variant: String): TaskProvider<Sync> {
    val classesDir = layout.buildDirectory
        .dir("intermediates/contractTestFixtures/$variant/classes")

    val sync = tasks.register<Sync>("publish${variant}ContractTestFixtures") {
        group = "build"
        description =
            "Copies the compiled Contract Test suite into core's published " +
                "testFixtures artifact."

        from(tasks.named("compile${variant}UnitTestKotlin")) {
            include(contractTestPackageGlob)
        }
        into(classesDir)
    }

    tasks.withType<JavaCompile>().configureEach {
        if (name == "compile${variant}TestFixturesJavaWithJavac") {
            dependsOn(sync)
            destinationDirectory.set(classesDir)
        }
    }

    return sync
}

// `ContractSuiteSelfTest` asserts that this publication actually happened and
// produced the suite, rather than assuming it: the failure mode this whole
// mechanism exists to prevent is a green build with an empty published artifact,
// and a test that reads the directory is the only thing that would notice it.
tasks.named<Test>("testDebugUnitTest") {
    dependsOn(publishDebugContractTestFixtures)
    systemProperty(
        "openflow.contractTestFixturesDir",
        layout.buildDirectory
            .dir("intermediates/contractTestFixtures/Debug/classes")
            .get()
            .asFile
            .absolutePath,
    )
}