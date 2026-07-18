plugins {
    id("org.gradle.kotlin.kotlin-dsl") version "6.6.4"
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven { url = uri("../offline-repository") }
}
