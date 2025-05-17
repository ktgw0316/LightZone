plugins {
    kotlin("jvm")
    id("lightzone.java-conventions")
}
dependencies {
    implementation(project(":lightcrafts"))
}
application {
    mainClass.set("com.lightcrafts.platform.linux.LinuxLauncher")
}
