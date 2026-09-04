import org.gradle.api.tasks.testing.Test

plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<Test> {
    useJUnit()
}
