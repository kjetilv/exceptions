import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    application
    id("com.github.johnrengelman.shadow")
}

tasks.withType<ShadowJar>() {
    mergeServiceFiles()
    relocate("com.fasterxml", "unearthly.xml")
}

application {
    applicationName = "feed"
    mainClassName = "no.scienta.unearth.client.main.Feed"
}

dependencies {
    compile("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    compile("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}
