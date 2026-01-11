plugins {
    kotlin("jvm")
    id("lightzone.java-conventions")
    id("org.beryx.runtime")
}
dependencies {
    implementation(project(":lightcrafts"))
}
application {
    mainClass.set("com.lightcrafts.platform.linux.LinuxLauncher")
}
tasks {
    build {
        dependsOn(":lightcrafts:build")
    }
    clean {
        dependsOn(":lightcrafts:clean")
    }
    jpackageImage {
        dependsOn("build")
        doFirst {
            val appDir = layout.buildDirectory.dir("jpackage/${project.name}/AppDir")
            val productsDir = layout.buildDirectory.dir("products")
            val iconsDir = layout.buildDirectory.dir("icons")
            copy {
                from(productsDir)
                include(${project.name}.desktop)
                into(appDir)
            }
            copy {
                from(iconsDir/hicolor/256x256/apps)
                include(${project.name}.png)
                into(appDir)
            }
            copy {
                from(productsDir)
                include("${project.name}")
                include("dcraw_lz")
                into(appDir/usr/bin)
            }
            copy {
                from(productsDir)
                include("*.so")
                into(appDir/usr/lib/${project.name})
            }
            copy {
                from(productsDir)
                include("*.jar")
                into(appDir/usr/share/${project.name}/lib)
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
        appVersion = version.toString().substringBefore("b") // Remove beta number
        imageName = ${project.name}
        mainJar = "${project.name}-${project.version}.jar"
        skipInstaller = true
        installerType = "app-image"
        imageOptions = listOf(
            "--icon", "icons/256x256/apps/lightzone.png"
        )
        jvmArgs = listOf(
            "--add-exports=java.desktop/sun.awt.image=ALL-UNNAMED",
            "-Djava.library.path=\$APPDIR",
            "-Dfile.encoding=utf-8",
        )
    }
}
