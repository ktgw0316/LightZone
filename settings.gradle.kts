import org.gradle.internal.os.OperatingSystem

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

include(":lightcrafts")
val os = OperatingSystem.current()!!
val osName = when {
    os.isWindows -> "windows"
    os.isMacOsX -> "macosx"
    else -> "linux"
}
include(osName)
