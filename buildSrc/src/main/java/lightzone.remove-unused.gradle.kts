plugins {
	java
}

afterEvaluate {
    tasks {
        named<Task>("jpackageImage") {
            dependsOn("build")

            doLast {
                val jarsToDelete = listOf(
                    "annotations*.jar",
                    "antlr4-runtime*.jar",
                    "bigint*.jar",
                    "checker-qual*.jar",
                    "commons-compiler*.jar",
                    "error_prone_annotations*.jar",
                    "failureaccess*.jar",
                    "guava*.jar",
                    "j2objc-annotations*.jar",
                    "janino*.jar",
                    "jsr305*.jar",
                    "jts-core*.jar",
                    "kotlin-stdlib*.jar",
                    "listenablefuture*.jar"
                )
                jarsToDelete.forEach { jar ->
                    delete(fileTree(layout.buildDirectory.dir("jpackage/lightzone/lib/app")).matching { include(jar) })
                }
            }
        }
    }
}
