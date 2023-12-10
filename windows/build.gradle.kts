dependencies {
    "implementation"(project(":lightcrafts"))
}
application {
    mainClass.set("com.lightcrafts.platform.windows.WindowsLauncher")
    tasks.run.get().workingDir = file("products")
}
