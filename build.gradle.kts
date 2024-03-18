plugins {
    java
    application
    kotlin("jvm") version "1.9.22"
    id("org.beryx.runtime") version "1.13.1" apply false
}
repositories {
    mavenCentral()
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "application")
    apply(plugin = "kotlin")
    group = "com.lightcrafts"
    version = rootProject.file("lightcrafts/version.txt").readText().trim().substringBefore('~')
    application {
        applicationName = "LightZone"
        applicationDefaultJvmArgs += listOf(
            "--add-exports=java.desktop/sun.awt.image=ALL-UNNAMED",
            "-Xmx512m",
        )
    }
    repositories {
        maven(url = "https://maven.geotoolkit.org/")
        mavenCentral()
    }
    dependencies {
        annotationProcessor("org.jetbrains:annotations:24.0.1")
        annotationProcessor("org.projectlombok:lombok:1.18.26")
        compileOnly("org.jetbrains:annotations:24.0.1")
        compileOnly("org.projectlombok:lombok:1.18.30")
        implementation(files("${project.rootDir}/lightcrafts/lib/jai-lightzone-1.1.3.0.jar"))
        testCompileOnly("org.jetbrains:annotations:24.0.1")
        testImplementation(kotlin("test"))
        testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
        testImplementation("org.assertj:assertj-core:3.11.1")
        testImplementation(platform("org.junit:junit-bom:5.10.1"))
        testImplementation("org.junit.jupiter:junit-jupiter-api")
        testImplementation("org.junit.jupiter:junit-jupiter-engine")
        testImplementation("org.junit.jupiter:junit-jupiter-params")
    }
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
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
            workingDir = file("products")
        }
        run.get().workingDir = file("products")
        register<Exec> ("products") {
            commandLine(MAKE, "-C", "products", "-j")
        }
        register<Exec> ("cleanProducts") {
            commandLine(MAKE, "-C", "products", "-j", "-s", "clean")
        }
        getByName("build") {
            dependsOn("products")
        }
        getByName("clean") {
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
            getByName("build") {
                dependsOn("jni")
            }
            getByName("clean") {
                dependsOn("cleanJni")
            }
        }
    }
}
