plugins {
    kotlin("jvm") version "2.2.0-RC2"
    kotlin("kapt") version "2.2.0-RC2"
    id("com.gradleup.shadow") version "9.0.0-beta17"
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.8"
}

group = "me.arcator"
version = "1.8.1"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    kapt("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("me.arcator.onfimLib:onfimLib:1.8.1")
    implementation("org.msgpack:jackson-dataformat-msgpack:0.9.9")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.0")
    implementation("com.fasterxml.jackson.core:jackson-core:2.19.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20250517")
    implementation("net.fellbaum:jemoji:1.7.4")
    implementation("de.themoep:minedown-adventure:1.7.4-SNAPSHOT")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.8.1")
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
