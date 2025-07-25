plugins {
    kotlin("jvm")
    id("lightzone.java-conventions")
    id("org.beryx.runtime")
}
dependencies {
    implementation(project(":lightcrafts"))
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
    jar {
        manifest {
            attributes(
                "Manifest-Version" to "1.0",
                "Main-Class" to application.mainClass,
                "SplashScreen-Image" to "com/lightcrafts/splash/resources/Splash.png"
            )
        }
    }
    register<Exec> ("helpFiles") {
        commandLine("make", "-C", "help")
    }
    jpackage {
        dependsOn("build", "helpFiles")
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
    }
}
runtime {
    options.set(listOf("--strip-debug", "--no-header-files", "--no-man-pages"))
    modules.set(
        listOf(
            "java.base", "java.desktop", "java.logging", "java.management", "java.prefs", "java.rmi", "jdk.management"
        )
    )
    jpackage {
        imageName = "LightZone"
        installerName = "LightZone-Installer"
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
