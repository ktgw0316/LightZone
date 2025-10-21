import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.*

plugins {
    application
    java
}

group = "com.lightcrafts"
version = rootProject.file("lightcrafts/version.txt").readText().trim().substringBefore('~')

repositories {
    maven(url = "https://maven.geotoolkit.org/")
    mavenCentral()
}

val jetbrainsAnnotation = "org.jetbrains:annotations:24.0.1"
val lombok = "org.projectlombok:lombok:1.18.38"
dependencies {
    annotationProcessor(jetbrainsAnnotation)
    annotationProcessor(lombok)
    compileOnly(jetbrainsAnnotation)
    compileOnly(lombok)
    implementation(files("${project.rootDir}/lightcrafts/lib/imagen-core-0.4-JAVA17-SNAPSHOT.jar"))
    implementation(files("${project.rootDir}/lightcrafts/lib/imagen-codec-0.4-JAVA17-SNAPSHOT.jar"))
    testCompileOnly(jetbrainsAnnotation)
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("org.assertj:assertj-core:3.11.1")
    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
}

application {
    applicationName = "LightZone"
    applicationDefaultJvmArgs += listOf(
        "--add-exports=java.desktop/sun.awt.image=ALL-UNNAMED",
        "-Xmx512m",
    )
}

val os = System.getProperty("os.name").lowercase()
val MAKE = with(os) {
    when {
        startsWith("sun") -> "gmake"
        endsWith("bsd") -> "gmake"
        else -> "make"
    }
}
tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs = listOf("-h", file("javah").absolutePath, "-proc:full")
    }
    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
        workingDir = file("products")
    }
    run.get().workingDir = file("products")
    register<Exec> ("products") {
        commandLine(MAKE, "-C", "products", "-j")
    }
    register<Exec> ("cleanProducts") {
        commandLine(MAKE, "-C", "products", "-j", "-s", "clean")
    }
    build {
        dependsOn("products")
    }
    clean {
        dependsOn("cleanProducts")
    }
    if (file("jnisrc").exists()) {
        register<Exec> ("jni") {
            dependsOn("classes")
            commandLine(MAKE, "-C", "jnisrc")
        }
        register<Exec> ("cleanJni") {
            commandLine(MAKE, "-C", "jnisrc", "-j", "-s", "distclean")
        }
        build {
            dependsOn("jni")
        }
        clean {
            dependsOn("cleanJni")
        }
    }
    if (os.contains("windows") && System.getenv("MSSDK_HOME") == null) {
        withType<Exec>().configureEach {
            val msSdkHome = "/c/Program Files (x86)/Windows Kits/10/Lib/10.0.22621.0"
            environment("MSSDK_HOME", msSdkHome)
        }
    }
}
