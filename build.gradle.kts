// import org.gradle.nativeplatform.platform.NativePlatform
// import org.gradle.nativeplatform.platform.OperatingSystem

plugins {
    java
    application
    kotlin("jvm") version "1.9.22"
}
repositories {
    mavenCentral()
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "application")
    apply(plugin = "kotlin")
    group = "com.lightcrafts"
    version = rootProject.file("lightcrafts/version.txt").readText().trim()
    application {
        applicationDefaultJvmArgs += listOf(
            "--add-exports=java.desktop/sun.awt.image=ALL-UNNAMED",
            "-Xmx512m",
            "-Dlensfun.dir=./share/lensfun",
        )
    }
    repositories {
        maven(url = "https://maven.geotoolkit.org/")
        mavenCentral()
    }
    dependencies {
        "annotationProcessor"("org.jetbrains:annotations:24.0.1")
        "annotationProcessor"("org.projectlombok:lombok:1.18.26")
        "compileOnly"("org.jetbrains:annotations:24.0.1")
        "compileOnly"("org.projectlombok:lombok:1.18.26")
        "implementation"(files("${project.rootDir}/lightcrafts/lib/jai-lightzone-1.1.3.0.jar"))
        "testCompileOnly"("org.jetbrains:annotations:24.0.1")
        "testImplementation"(kotlin("test"))
        "testImplementation"("io.kotest:kotest-runner-junit5:5.8.0")
        "testImplementation"("org.assertj:assertj-core:3.11.1")
        "testImplementation"(platform("org.junit:junit-bom:5.10.1"))
        "testImplementation"("org.junit.jupiter:junit-jupiter-api")
        "testImplementation"("org.junit.jupiter:junit-jupiter-engine")
        "testImplementation"("org.junit.jupiter:junit-jupiter-params")
    }
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
    // val os = NativePlatform.getOperatingSystem
    // val MAKE = when(os) {
    //     isFreeBSD, isSolaris -> "gmake"
    //     else -> "make"
    // }
    val MAKE = "make"
    tasks {
        withType<JavaCompile> {
            options.encoding = "UTF-8"
            options.compilerArgs = listOf("-h", file("javah").absolutePath)
        }
        test {
            useJUnitPlatform()
            testLogging {
                events("passed", "skipped", "failed")
            }
        }
        register<Exec> ("products") {
            commandLine(MAKE, "-C", "products", "-j")
        }
        register<Exec> ("cleanProducts") {
            commandLine(MAKE, "-C", "products", "-j", "-s", "clean")
        }
        register<Exec> ("jni") {
            dependsOn("classes")
            commandLine(MAKE, "-C", "jnisrc")
        }
        register<Exec> ("cleanJni") {
            commandLine(MAKE, "-C", "jnisrc", "-j", "-s", "clean")
        }
        getByName("build") {
            dependsOn("jni", "products")
        }
        getByName("clean") {
            dependsOn("cleanProducts", "cleanJni")
        }
    }
}
