import org.gradle.internal.os.OperatingSystem

plugins {
    kotlin("jvm") version "2.4.10-RC" apply false
    id("org.beryx.runtime") version "2.0.1" apply false
}

val os = OperatingSystem.current()!!
val osName = when {
    os.isWindows -> "windows"
    os.isMacOsX -> "macosx"
    else -> "linux"
}

rootProject.name = "lightzone"
include("lightcrafts", osName)
