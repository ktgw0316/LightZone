plugins {
    kotlin("jvm")
    id("lightzone.java-conventions")
    id("org.beryx.runtime")
}
dependencies {
    implementation(project(":lightcrafts"))
    implementation("javax.help:javahelp:2.0.05")
}
application {
    mainClass.set("com.lightcrafts.platform.linux.LinuxLauncher")
}
tasks {
    named("build") {
        dependsOn(":lightcrafts:build")
    }
    named("clean") {
        dependsOn(":lightcrafts:clean")
    }
    register<Exec> ("helpFiles") {
        commandLine("ant", "-f", "help/build.xml")
    }
    named("jpackage") {
        dependsOn("build", "helpFiles")
        doFirst {
            copy {
                from("products/")
                include("dcraw_lz")
                include("LightZone-forkd")
                into(layout.buildDirectory.dir("jpackage/lightzone/bin"))
            }
            copy {
                from("products/")
                include("*.so")
                into(layout.buildDirectory.dir("jpackage/lightzone/lib/app"))
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
        imageName = "lightzone"
        installerName = "lightzone"
        skipInstaller = false
        installerOptions = listOf("--vendor", "LightZone Project")
        imageOptions = listOf("--icon", "icons/hicolor/256x256/apps/lightzone.png")
        installerType = "deb"
    }
    launcher {
        jvmArgs = listOf(
            "--add-exports=java.desktop/sun.awt.image=ALL-UNNAMED",
            "-Djava.library.path=\$APPDIR"
        )
        runInBinDir = true
    }
}