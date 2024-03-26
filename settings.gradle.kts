import org.gradle.internal.os.OperatingSystem

plugins {
    kotlin("jvm") version "1.9.22" apply false
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
