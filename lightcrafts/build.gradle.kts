import java.io.FileOutputStream

val MAKE = "make"

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
dependencies {
    "implementation"("com.formdev:flatlaf:3.1.1")
    "implementation"("com.formdev:flatlaf-intellij-themes:3.1.1")
    "implementation"("com.github.jiconfont:jiconfont-swing:1.0.1")
    "implementation"("com.github.jiconfont:jiconfont-font_awesome:4.7.0.0")
    "implementation"("com.github.jiconfont:jiconfont-google_material_design_icons:2.2.0.2")
    "implementation"("org.ejml:ejml-simple:0.40")
    "implementation"("org.json:json:20231013")
}
tasks {
    register<Exec> ("coprocesses") {
        commandLine(MAKE, "-C", "coprocesses", "-j", "-s")
    }
    register<Exec> ("cleanCoprocesses") {
        commandLine(MAKE, "-C", "coprocesses", "-j", "-s", "clean")
    }
    register<Task> ("revision") {
        val dirProvider = layout.buildDirectory.dir("resources/main/com/lightcrafts/utils/resources")
        val dir = dirProvider.get().asFile
        mkdir(dir)
        val file = File("$dir/Revision")
        file.delete()
        FileOutputStream(file).use {
            project.exec {
                commandLine("git", "rev-parse", "HEAD")
                standardOutput = it
            }
            it.toString().trim()
        }
        File("$dir/Version").writeText(version.toString())
    }
    getByName("build") {
        dependsOn("coprocesses", "revision")
    }
    getByName("clean") {
        dependsOn("cleanCoprocesses")
    }
    withType<JavaExec> {
        systemProperty("java.library.path", "${projectDir}/products")
    }
}
