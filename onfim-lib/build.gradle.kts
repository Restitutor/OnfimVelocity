plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // Additional dependencies specific to lib module
    compileOnly(libs.bundles.jackson)
    compileOnly(libs.bundles.common.libs)
    compileOnly("net.kyori:adventure-api:4.21.0")
}
