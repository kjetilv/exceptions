import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow")
    kotlin("jvm")
}

tasks.withType<ShadowJar>() {
    manifest {
        attributes(
                mapOf(Pair("Main-Class", "no.scienta.unearth.main.MainKt"))
        )
    }
    mergeServiceFiles()
}

dependencies {
    compile(project(":unearth-server"))
    implementation(kotlin("stdlib"))
}
