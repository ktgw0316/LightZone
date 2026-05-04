import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    kotlin("jvm")
    id("lightzone.java-conventions")
    id("lightzone.remove-unused")
    id("org.beryx.runtime")
}
dependencies {
    implementation(project(":lightcrafts"))
    implementation("org.eclipse.imagen:imagen-all:0.9.1")
}
application {
    mainClass.set("com.lightcrafts.platform.windows.WindowsLauncher")
}
tasks {
    build {
        dependsOn(":lightcrafts:build")
    }
    clean {
        dependsOn(":lightcrafts:clean")
    }
    register<Exec> ("helpFiles") {
        commandLine("make", "-C", "help")
    }
    jpackage {
        dependsOn("jpackageImage", "helpFiles")
        doFirst {
            copy {
                from("products/")
                include("*.chm")
                into(layout.buildDirectory.dir("jpackage/LightZone"))
            }
            copy {
                from("products/")
                include("share/**")
                include("dcraw_lz*")
                include("*.dll")
                into(layout.buildDirectory.dir("jpackage/LightZone/app"))
            }
        }
        doLast {
            val versionSuffix = "-${project.version}.msi"
            val installerDir = layout.buildDirectory.dir("jpackage").get().asFile
            installerDir.walkTopDown()
                .filter { it.isFile && it.extension.equals("msi", ignoreCase = true) }
                .filter { it.name.endsWith(versionSuffix) }
                .forEach { installer ->
                    val renamed = installer.resolveSibling(installer.name.removeSuffix(versionSuffix) + ".msi")
                    if (!renamed.exists()) {
                        installer.renameTo(renamed)
                    }
                }
        }
    }
}
runtime {
    val fullVersion = project.version.toString()
    val buildTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
    val osArch = System.getProperty("os.arch").lowercase()
    val archName = when (osArch) {
        "x86", "i386" -> "32bit"
        "x64", "amd64", "x86_64" -> "64bit"
        else -> osArch
    }

    options.set(listOf("--strip-debug", "--no-header-files", "--no-man-pages"))
    modules.set(
        listOf(
            "java.base", "java.desktop", "java.logging", "java.management", "java.naming", "java.prefs", "java.rmi", "jdk.management"
        )
    )
    jpackage {
        imageName = "LightZone"
        installerName = "LightZone-$fullVersion-$buildTimestamp-windows-$archName"
        skipInstaller = false
        installerOptions = listOf(
            "--type", "msi",
            "--vendor", "LightZone Project",
            "--win-dir-chooser",
            "--win-menu",
            "--win-menu-group", "LightZone",
            "--win-shortcut",
            "--win-shortcut-prompt",
            "--win-upgrade-uuid", "8d785df3-7ed8-41f9-8fdb-d0f0f67ee2e9",
        )
        imageOptions = listOf("--icon", "src/main/resources/icons/LightZone.ico")
    }
    launcher {
        jvmArgs = listOf("--add-exports=java.desktop/sun.awt.image=ALL-UNNAMED")
    }
}
