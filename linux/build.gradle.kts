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
    jpackage {
        dependsOn("jpackageImage")
    }
    jpackageImage {
        dependsOn("build")
        doLast {
            copy {
                from("products/")
                include("dcraw_lz")
                into(layout.buildDirectory.dir("jpackage/lightzone/bin"))
            }
            copy {
                from("products/")
                include("*.so")
                into(layout.buildDirectory.dir("jpackage/lightzone/lib/app"))
            }
            copy {
                from("products/")
                include("lightzone.desktop")
                into(layout.buildDirectory.dir("jpackage/lightzone/share/applications"))
            }
            copy {
                from("icons")
                include("**")
                into(layout.buildDirectory.dir("jpackage/lightzone/share/icons"))
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
        imageName = "lightzone"
        installerName = "lightzone-installer"
        mainJar = "${project.name}-${project.version}.jar"
        skipInstaller = false
        installerType = "deb"
        installerOptions = listOf(
            "--vendor", "LightZone Project",
            "--linux-package-name", "lightzone",
            "--linux-deb-maintainer", "arctica0316@gmail.com",
            "--linux-menu-group", "Photography",
            "--linux-package-deps",
            "default-jre (>= 2:1.17) | openjdk-17-jre, libejml-java (>= 0.38), libflatlaf-java, libgomp1, libjiconfont-font-awesome-java, libjiconfont-google-material-design-icons-java, libjiconfont-swing-java, libjpeg62-turbo | libjpeg-turbo8, liblcms2-2, liblensfun0 | liblensfun1 (<< 0.3.95), libopenjson-java, libraw19 | libraw20 | libraw23, libtiff5 | libtiff6, libxml2-utils",
            "--linux-app-release", "${project.version}",
            "--linux-app-category", "graphics",
            "--linux-rpm-license-type", "BSD-3-Clause",
        )
        imageOptions = listOf(
            "--icon", "icons/hicolor/256x256/apps/lightzone.png",
        )
    }
    launcher {
        jvmArgs = listOf(
            "--add-exports=java.desktop/sun.awt.image=ALL-UNNAMED",
            "-Djava.library.path=\$APPDIR",
            "-Dfile.encoding=utf-8",
        )
    }
}
