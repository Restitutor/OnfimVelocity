plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.shadow)
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.8"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    compileOnly(libs.velocity.api)
    kapt(libs.velocity.api)

    implementation(libs.tzbot4j)
    implementation(libs.kotlin.stdlib)
    implementation(project(":onfim-lib"))
    implementation(libs.bundles.jackson)
    implementation(libs.bundles.common.libs)
    implementation(libs.kotlinx.serialization)
}

tasks.build { dependsOn(tasks.shadowJar) }
