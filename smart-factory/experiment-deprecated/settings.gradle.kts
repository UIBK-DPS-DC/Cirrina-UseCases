rootProject.name = "smart_factory"

val cirrinaHome: String? = System.getenv("CIRRINA_HOME")

if (cirrinaHome.isNullOrBlank()) {
    throw IllegalStateException("CIRRINA_HOME environment variable is not set.")
}

include("cirrina")
project(":cirrina").projectDir = file("$cirrinaHome")

println("Cirrina Home: " + project(":cirrina").projectDir)