plugins {
    id("org.beryx.runtime")
}
dependencies {
    "implementation"(project(":lightcrafts"))
}
application {
    mainClass.set("com.lightcrafts.platform.windows.WindowsLauncher")
}
tasks {
    named("build") {
        dependsOn(":lightcrafts:build")
    }
    named("clean") {
        dependsOn(":lightcrafts:clean")
    }
    named("jpackage") {
        dependsOn("build")
        doFirst {
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
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
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
            "--vendor", "LightZone Project",
            "--win-dir-chooser",
            "--win-menu",
            "--win-menu-group", "LightZone",
            "--win-shortcut",
            "--win-upgrade-uuid", "8d785df3-7ed8-41f9-8fdb-d0f0f67ee2e9",
        )
        imageOptions = listOf("--icon", "src/main/resources/icons/LightZone.ico")
    }
    launcher {
        jvmArgs = listOf("--add-exports=java.desktop/sun.awt.image=ALL-UNNAMED")
    }
}