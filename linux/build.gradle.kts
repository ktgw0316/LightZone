dependencies {
    "implementation"(project(":lightcrafts"))
    "implementation"("javax.help:javahelp:2.0.05")
}
application {
    mainClass.set("com.lightcrafts.platform.linux.LinuxLauncher")
}
