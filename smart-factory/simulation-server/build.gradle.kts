import com.google.protobuf.gradle.id

plugins {
    application
    id("java")
    id("com.google.protobuf") version "0.9.4"
}

group = "at.ac.uibk.dps.smartfactory"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

protobuf {
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                id("python")
                id("cpp")
            }
        }
    }
}

repositories {
    mavenCentral()
    maven(url = "https://repository.cloudera.com/artifactory/cloudera-repos/")
}

dependencies {
    implementation("com.beust:jcommander:1.82")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.1")

    implementation("com.google.protobuf:protobuf-java:3.25.3")

    implementation("org.apache.logging.log4j:log4j-core:2.23.1")
}

tasks {

    task("runMain", type = JavaExec::class) {
        mainClass = "at.ac.uibk.dps.smartfactory.Main"
        classpath = sourceSets["main"].runtimeClasspath
    }
}
