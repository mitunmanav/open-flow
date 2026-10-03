// OpenFlow V1 module graph. ADR-0005 fixes the direction:
//   app  ──►  core   ◄──   providers/sherpa
// `core` never depends on a provider or on `app`. Adding a provider means adding a
// line here and one registry entry in `app/`; adding a feature means a new package
// in `core/`, never a new module. See docs/architecture/modules.md.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // ADR-0005's dependency rule is only enforceable if nothing can quietly add a
    // repository at module level. FAIL_ON_PROJECT_REPOS makes that a build failure
    // instead of a review comment.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // sherpa-onnx is not on Maven Central — JitPack is its only Maven-coordinate
        // distribution channel. Do not remove this without a replacement; see
        // ADR-0007 for why this coordinate is pinned to a tag.
        maven("https://jitpack.io")
    }
}

rootProject.name = "OpenFlow"

include(":app")
include(":core")
include(":providers:sherpa")