// Plugins are declared here with `apply false` so each module gets the same
// version from the catalogue, and no module is built by applying a root plugin.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
}