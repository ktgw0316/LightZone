import java.io.FileOutputStream

plugins {
    kotlin("jvm") version "1.9.21"
}

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
dependencies {
    implementation(kotlin("stdlib-jdk8"))
}
repositories {
    mavenCentral()
}
kotlin {
    jvmToolchain(17)
}