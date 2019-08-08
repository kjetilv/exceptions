import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow")
}

tasks.withType<ShadowJar>() {
    manifest {
        attributes(
                mapOf(Pair("Main-Class", "no.scienta.unearth.client.main.Feed"))
        )
    }
    mergeServiceFiles()
    relocate("com.fasterxml", "unearthly.xml")
}

dependencies {
    compile("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    compile("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}
