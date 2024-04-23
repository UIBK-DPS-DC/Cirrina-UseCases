rootProject.name = "smart_factory"

include("core")
project(":core").projectDir = file("../../../cirrina/core") //TODO Change
println("Core: " + project(":core").projectDir)

include("runtime")
project(":runtime").projectDir = file("../../../cirrina/runtime") //TODO Change
println("Runtime: " + project(":runtime").projectDir)