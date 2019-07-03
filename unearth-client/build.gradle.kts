import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

application {
    mainClassName = "no.scienta.unearth.client.SubmitMain"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

dependencies {
    compile(project(":unearth-core"))
    compile(project(":unearth-dto"))
    compile(project(":unearth-munch"))

    compile("com.squareup.retrofit2:retrofit:2.6.0")
    compile("com.squareup.retrofit2:converter-jackson:2.6.0")

    compile("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.9.9")
    compile("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.9.9")
    compile("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.9")
}
