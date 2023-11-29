import java.io.FileOutputStream

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
    val mesonPrefixDir = layout.buildDirectory.dir("products").get().asFile.absolutePath
    register<Exec> ("dcraw0") {
        workingDir("coprocesses/dcraw")
        commandLine("meson", "setup", "--prefix", mesonPrefixDir, "--bindir", ".", "--buildtype", "release", "build")
    }
    register<Exec> ("dcraw") {
        dependsOn("dcraw0")
        workingDir("coprocesses/dcraw/build")
        commandLine("meson", "install")
    }
    register<Exec> ("forkdaemon0") {
        workingDir("coprocesses/forkdaemon")
        commandLine("meson", "setup", "--prefix", mesonPrefixDir, "--bindir", ".", "--buildtype", "release", "build")
    }
    register<Exec> ("forkdaemon") {
        dependsOn("forkdaemon0")
        workingDir("coprocesses/forkdaemon/build")
        commandLine("meson", "install")
    }
    register<Task> ("coprocesses") {
        dependsOn("dcraw", "forkdaemon")
    }
    getByName("cleanCoprocesses", Delete::class) {
        delete = setOf("coprocesses/dcraw/build", "coprocesses/forkdaemon/build")
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
