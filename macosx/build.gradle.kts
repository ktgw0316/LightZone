plugins {
    id("org.beryx.runtime")
}
dependencies {
    "implementation"(project(":lightcrafts"))
}
application {
    mainClass.set("com.lightcrafts.platform.macosx.MacOSXLauncher")
}
tasks {
    named("build") {
        dependsOn(":lightcrafts:build")
    }
    named("clean") {
        dependsOn(":lightcrafts:clean")
    }
    register<Exec> ("helpFiles") {
        commandLine("make", "-C", "help")
    }
    named("jpackage") {
        dependsOn("build", "helpFiles")
        doFirst {
            copy {
                from(layout.buildDirectory.file("libs/${project.name}-${project.version}.jar"))
                into(layout.buildDirectory.dir("jpackage"))
            }
            copy {
                from("products/")
                include("share/**")
                include("dcraw_lz")
                include("LightZone-forkd")
                include("*.dylib")
                include("*.jnilib")
                into(layout.buildDirectory.dir("jpackage/LightZone.app/Contents/app"))
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
        mainJar = "${project.name}-${project.version}.jar"
        skipInstaller = false
        installerOptions = listOf("--vendor", "LightZone Project")
        imageOptions = listOf("--icon", "src/main/resources/Resources/LightZone.icns")
    }
    launcher {
        jvmArgs = listOf(
            "--add-exports=java.desktop/sun.awt.image=ALL-UNNAMED",
            "-Dapple.awt.graphics.UseQuartz=false",
            "-Dapple.laf.useScreenMenuBar=true",
            "-Dcom.apple.macos.use-file-dialog-packages=true",
            "-Dcom.apple.macos.useScreenMenuBar=true",
            "-Djava.library.path=\$APPDIR",
            "-Dfile.encoding=utf-8",
            "-Xdock:name=LightZone",
        )
    }
}
