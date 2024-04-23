rootProject.name = "smart_factory"

val cirrinaHome: String? = System.getenv("CIRRINA_HOME")

if (cirrinaHome.isNullOrBlank()) {
    throw IllegalStateException("CIRRINA_HOME environment variable is not set.")
}

include("core")
project(":core").projectDir = file("$cirrinaHome/core")

println("Core: " + project(":core").projectDir)

include("runtime")
project(":runtime").projectDir = file("$cirrinaHome/runtime")

println("Runtime: " + project(":runtime").projectDir)