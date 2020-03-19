import org.gradle.internal.os.OperatingSystem

include(":lightcrafts")
val os = OperatingSystem.current()!!
val osName = when {
    os.isWindows -> "windows"
    os.isMacOsX -> "macosx"
    else -> "linux"
}
include(osName)
