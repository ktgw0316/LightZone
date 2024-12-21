plugins {
    `kotlin-dsl`
}
repositories {
    gradlePluginPortal()
    mavenCentral()
}
dependencies {
    implementation("com.palantir.git-version:com.palantir.git-version.gradle.plugin:3.1.0")
}
