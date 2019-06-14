plugins {
    application
    id("com.github.johnrengelman.shadow")
}

application {
    mainClassName = "no.scienta.unearth.client.SubmitMain"
}

dependencies {
    compile(project(":unearth-core"))
    compile(project(":unearth-dto"))

    compile("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.9.9")
    compile("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.9.9")
}
