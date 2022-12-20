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
tasks {
    register<Exec> ("coprocesses") {
        commandLine(MAKE, "-C", "coprocesses", "-j", "-s")
    }
    register<Exec> ("cleanCoprocesses") {
        commandLine(MAKE, "-C", "coprocesses", "-j", "-s", "clean")
    }
    register<Task> ("revision") {
        val dir = "${project.buildDir}/resources/main/com/lightcrafts/utils/resources"
        mkdir(dir)
        val file = File("$dir/Revision")
        file.delete()
        FileOutputStream(file).use {
            project.exec {
                commandLine("git", "log", "-1")
                standardOutput = it
            }
            it.toString().trim()
        }
        FileOutputStream(file, true).use {
            project.exec {
                commandLine("git", "config", "--list")
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
