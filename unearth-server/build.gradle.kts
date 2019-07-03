import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    id("com.github.johnrengelman.shadow")
    kotlin("jvm")
}

application {
    mainClassName = "no.scienta.unearth.server.MainKt"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

dependencies {

    compile(project(":unearth-core"))
    compile(project(":unearth-dto"))
    compile(project(":unearth-metrics"))
    compile(project(":unearth-static"))

    compile("org.http4k:http4k-core:3.159.0")
    compile("org.http4k:http4k-contract:3.159.0")

    compile("org.http4k:http4k-format-jackson:3.159.0")
    compile("org.http4k:http4k-server-netty:3.159.0")

    compile("com.natpryce:konfig:1.6.10.0")

    compile("io.swagger.core.v3:swagger-core:2.0.8")
    runtime("org.webjars:swagger-ui:3.22.2")

    compile("io.micrometer:micrometer-registry-jmx:1.1.4")

    compile("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.9.9")
    compile("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.9.9")

    runtime("ch.qos.logback:logback-classic:1.3.0-alpha4")

    testCompile("org.http4k:http4k-client-apache:3.159.0")
    testCompile("junit:junit:4.12")

    implementation(kotlin("stdlib"))
}
