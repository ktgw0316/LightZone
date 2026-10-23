plugins {
    id("org.gradle.kotlin.kotlin-dsl") version "6.7.6"
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven { url = uri("../offline-repository") }
}
