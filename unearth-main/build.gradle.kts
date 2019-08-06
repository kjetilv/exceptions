plugins {
    application
    id("com.github.johnrengelman.shadow")
    kotlin("jvm")
}

application {
    mainClassName = "no.scienta.unearth.main.MainKt"
}

dependencies {
    compile(project(":unearth-server"))
    implementation(kotlin("stdlib"))
}
