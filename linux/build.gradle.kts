import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    kotlin("jvm")
    id("lightzone.java-conventions")
    id("org.beryx.runtime")
}
dependencies {
    implementation(project(":lightcrafts"))
    implementation("org.slf4j:slf4j-api:2.0.17")
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

            val jarsToDelete = listOf(
                "annotations*.jar",
                "antlr4-runtime*.jar",
                "bigint*.jar",
                "checker-qual*.jar",
                "commons-compiler*.jar",
                "error_prone_annotations*.jar",
                "failureaccess*.jar",
                "guava*.jar",
                "j2objc-annotations*.jar",
                "janino*.jar",
                "jsr305*.jar",
                "jts-core*.jar",
                "kotlin-stdlib*.jar",
                "listenablefuture*.jar"
            )
            jarsToDelete.forEach { jar ->
                delete(fileTree(layout.buildDirectory.dir("jpackage/lightzone/lib/app")).matching { include(jar) })
            }
        }
    }
}
runtime {
    val fullVersion = project.version.toString()
    val buildTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))

    options.set(listOf("--strip-debug", "--no-header-files", "--no-man-pages"))
    modules.set(
        listOf(
            "java.base", "java.desktop", "java.logging", "java.management", "java.naming", "java.prefs", "java.rmi", "jdk.management"
        )
    )
    jpackage {
        version = fullVersion + "+" + buildTimestamp
        imageName = "lightzone"
        mainJar = "${project.name}-${project.version}.jar"
        skipInstaller = false
        installerType = "deb"
        installerOptions = listOf(
            "--vendor", "LightZone Project",
            "--linux-package-name", "lightzone",
            "--linux-deb-maintainer", "arctica0316@gmail.com",
            "--linux-menu-group", "Photography",
            "--linux-package-deps",
            "default-jre (>= 2:1.17) | openjdk-21-jre, libgomp1, libjpeg62-turbo | libjpeg-turbo8, liblcms2-2, liblensfun0 | liblensfun1 (<< 0.3.95), libraw19 | libraw20 | libraw23, libtiff5 | libtiff6, libxml2-utils", // .deb
            // "java-21-openjdk, lcms2, lensfun, LibRaw, libxml2", // .rpm
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
