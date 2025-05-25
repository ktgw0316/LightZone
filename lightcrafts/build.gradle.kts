plugins {
    kotlin("jvm")
    id("com.palantir.git-version")
    id("lightzone.java-conventions")
}
dependencies {
    implementation("com.formdev:flatlaf:3.6")
    implementation("com.formdev:flatlaf-intellij-themes:3.6")
    implementation("com.github.jiconfont:jiconfont-swing:1.0.1")
    implementation("com.github.jiconfont:jiconfont-font_awesome:4.7.0.0")
    implementation("com.github.jiconfont:jiconfont-google_material_design_icons:2.2.0.2")
    implementation("com.github.openjson:openjson:1.0.13")
    implementation("org.ejml:ejml-simple:0.40")
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
tasks {
    register<Exec> ("coprocesses") {
        commandLine(MAKE, "-C", "coprocesses", "-j", "-s")
    }
    register<Exec> ("cleanCoprocesses") {
        commandLine(MAKE, "-C", "coprocesses", "-j", "-s", "clean")
    }
    register<Task> ("revision") {
        val gitHash = versionDetails().gitHashFull // full 40-character Git commit hash
        project.logger.lifecycle("Git hash: ${gitHash}")

        val dirProvider = layout.buildDirectory.dir("resources/main/com/lightcrafts/utils/resources")
        val dir = dirProvider.get().asFile
        mkdir(dir)
        val file = File("$dir/Revision")
        file.delete()
        file.writeText(gitHash)
        File("$dir/Version").writeText(version.toString())
    }
    build {
        dependsOn("coprocesses", "revision")
    }
    clean {
        dependsOn("cleanCoprocesses")
    }
    processResources {
        from("products/") {
            include("share/**")
            include("dcraw_lz*")
            include("*.dll")
            include("*.dylib")
            include("*.jnilib")
            into("native")
        }
    }
}
