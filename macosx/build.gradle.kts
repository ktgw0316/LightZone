plugins {
    kotlin("jvm")
    id("lightzone.java-conventions")
    id("org.beryx.runtime")
}
dependencies {
    implementation(project(":lightcrafts"))
}
application {
    mainClass.set("com.lightcrafts.platform.macosx.MacOSXLauncher")
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
        dependsOn("build", "helpFiles")
        doFirst {
            copy {
                from("products/")
                include("share/**")
                include("dcraw_lz")
                include("LightZone-forkd")
                include("*.dylib")
                include("*.jnilib")
                into(layout.buildDirectory.dir("jpackage/LightZone.app/Contents/app"))
            }
            copy {
                from(layout.buildDirectory.dir("resources/main/Resources"))
                include("*.lproj/**")
                include("*.icns")
                into(layout.buildDirectory.dir("jpackage/LightZone.app/Contents/Resources"))
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
        version = version.toString().substringBefore("b") // Remove beta number
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
