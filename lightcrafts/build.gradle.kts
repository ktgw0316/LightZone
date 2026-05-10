plugins {
    kotlin("jvm")
    id("com.palantir.git-version")
    id("lightzone.java-conventions")
}
kotlin {
    jvmToolchain(21)
}
dependencies {
    implementation("com.formdev:flatlaf:3.7")
    implementation("com.formdev:flatlaf-intellij-themes:3.7")
    implementation("com.github.jiconfont:jiconfont-swing:1.0.1")
    implementation("com.github.jiconfont:jiconfont-font_awesome:4.7.0.0")
    implementation("com.github.jiconfont:jiconfont-google_material_design_icons:2.2.0.2")
    implementation("com.github.openjson:openjson:1.0.13")
    implementation("org.eclipse.imagen:affine:0.9.1")
    implementation("org.eclipse.imagen:algebra:0.9.1")
    implementation("org.eclipse.imagen:bandcombine:0.9.1")
    implementation("org.eclipse.imagen:bandmerge:0.9.1")
    implementation("org.eclipse.imagen:bandselect:0.9.1")
    implementation("org.eclipse.imagen:crop:0.9.1")
    implementation("org.eclipse.imagen:format:0.9.0")
    implementation("org.eclipse.imagen:imagen-core:0.9.1")
    implementation("org.eclipse.imagen:lookup:0.9.1")
    implementation("org.eclipse.imagen:nullop:0.9.1")
    implementation("org.eclipse.imagen:rescale:0.9.1")
    implementation("org.ejml:ejml-simple:0.44.0")
}
application {
    mainClass.set("com.lightcrafts.app.Application")
}
sourceSets {
    main {
        resources {
            srcDir("src/main/locale")
            exclude("**/.git")
        }
    }
}
val os = System.getProperty("os.name").lowercase()
val MAKE = with(os) {
    when {
        startsWith("sun") -> "gmake"
        endsWith("bsd") -> "gmake"
        else -> "make"
    }
}
val versionDetails: groovy.lang.Closure<com.palantir.gradle.gitversion.VersionDetails> by extra
val nativeLibPath = layout.buildDirectory.dir("resources/main/native").get().asFile.absolutePath
tasks {
    // Disable run task since this is a library project, not a standalone application
    named("run") {
        enabled = false
    }
    register<Exec> ("coprocesses") {
        commandLine(MAKE, "-C", "coprocesses", "-j", "-s")
        doLast {
            copy {
                from("products/")
                include("dcraw_lz*")
                into(nativeLibPath)
            }
        }
    }
    register<Exec> ("cleanCoprocesses") {
        commandLine(MAKE, "-C", "coprocesses", "-j", "-s", "clean")
        doLast {
            val dcraw = File(nativeLibPath, "dcraw_lz*")
            if (dcraw.exists()) {
                dcraw.delete()
            }
        }
    }
    jni {
        doLast {
            copy {
                from("products/")
                include("share/**")
                include("*.dll")
                include("*.dylib")
                include("*.jnilib")
                include("*.so")
                into(nativeLibPath)
            }
        }
    }
    cleanJni {
        doLast {
            val nativeFiles = File(nativeLibPath).listFiles { _, name ->
                name.endsWith(".dll") || name.endsWith(".dylib") || name.endsWith(".jnilib") || name.endsWith(".so")
                        || name == "share"
            }
            nativeFiles?.forEach { it.deleteRecursively() }
        }
    }
    register<Task> ("revision") {
        try {
            val gitHash = versionDetails().gitHashFull // full 40-character Git commit hash
            project.logger.lifecycle("Git hash: ${gitHash}")

            val dirProvider = layout.buildDirectory.dir("resources/main/com/lightcrafts/utils/resources")
            val dir = dirProvider.get().asFile
            mkdir(dir)
            val file = File(dir, "Revision")
            if (file.exists()) file.delete()
            file.writeText(gitHash)
            File(dir, "Version").writeText(version.toString())
        } catch (e: Exception) {
            project.logger.lifecycle("Skipping revision task: unable to determine git hash: ${e.message}")
        }
    }
    build {
        dependsOn("coprocesses", "revision")
    }
    test {
        dependsOn("jni")
        jvmArgs("-Djava.library.path=$nativeLibPath")
    }
    clean {
        dependsOn("cleanCoprocesses", "cleanJni")
    }
}
