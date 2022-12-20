// import org.gradle.nativeplatform.platform.NativePlatform
// import org.gradle.nativeplatform.platform.OperatingSystem

plugins {
    java
    application
//    id("kotlin") version "1.3.70" apply false
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "application")
//    apply(plugin = "kotlin")
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
        maven(url = "https://jitpack.io/")
        mavenCentral()
    }
    dependencies {
        "annotationProcessor"("org.jetbrains:annotations:20.1.0")
        "annotationProcessor"("org.projectlombok:lombok:1.18.20")
        "compileOnly"("org.jetbrains:annotations:20.1.0")
        "compileOnly"("org.projectlombok:lombok:1.18.20")
        "implementation"(files("${project.rootDir}/lightcrafts/lib/substance-lite.jar"))
        "implementation"(files("${project.rootDir}/lightcrafts/lib/laf-widget-.jar"))
        "implementation"("javax.media:jai_core:1.1.3")
        "implementation"("javax.media:jai_codec:1.1.3")
        "implementation"("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
        "implementation"("com.formdev:flatlaf:2.6")
        "implementation"("com.formdev:flatlaf-intellij-themes:2.6")
        "implementation"("com.github.jiconfont:jiconfont-swing:1.0.1")
        "implementation"("com.github.jiconfont:jiconfont-font_awesome:4.7.0.0")
        "implementation"("com.github.jiconfont:jiconfont-google_material_design_icons:2.2.0.2")
        "implementation"("org.ejml:ejml-simple:0.40")
//        "implementation"(kotlin("stdlib-jdk8", "1.3.70"))
        "testImplementation"("junit:junit:4.12")
    }
    configure<JavaPluginConvention> {
        sourceCompatibility = JavaVersion.VERSION_11
    }
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs = listOf("-h", file("javah").absolutePath)
    }
    // val os = NativePlatform.getOperatingSystem
    // val MAKE = when(os) {
    //     isFreeBSD, isSolaris -> "gmake"
    //     else -> "make"
    // }
    val MAKE = "make"
    tasks {
//        compileKotlin {
//            kotlinOptions.jvmTarget = "1.8"
//        }
//        compileTestKotlin {
//            kotlinOptions.jvmTarget = "1.8"
//        }
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
