plugins {
    id("org.gradle.kotlin.kotlin-dsl") version "6.6.4"
}
repositories {
    gradlePluginPortal()
    mavenCentral()
}
dependencies {
    implementation("com.palantir.git-version:com.palantir.git-version.gradle.plugin:3.1.0")
}
