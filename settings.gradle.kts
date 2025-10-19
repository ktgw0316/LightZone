import org.gradle.internal.os.OperatingSystem

plugins {
    kotlin("jvm") version "2.2.0" apply false
    id("org.beryx.runtime") version "1.13.1" apply false
}

val os = OperatingSystem.current()!!
val osName = when {
    os.isWindows -> "windows"
    os.isMacOsX -> "macosx"
    else -> "linux"
}

rootProject.name = "lightzone"
include("lightcrafts", osName)
