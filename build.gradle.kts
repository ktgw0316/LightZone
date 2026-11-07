import org.gradle.internal.os.OperatingSystem

val os = OperatingSystem.current()!!
val osName = when {
    os.isWindows -> "windows"
    os.isMacOsX -> "macosx"
    else -> "linux"
}

// Delegate tasks to the OS-specific subproject
tasks {
    register("run") {
        dependsOn(":$osName:run")
        group = "application"
        description = "Runs the application (delegates to :$osName:run)"
    }
    register("prepareHostNativeLibraries") {
        dependsOn(":$osName:prepareNativeLibraries")
    }
}

