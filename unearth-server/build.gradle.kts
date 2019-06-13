plugins {
    application
    id("com.github.johnrengelman.shadow")
    kotlin("jvm")
}

application {
    mainClassName = "no.scienta.unearth.server.MainKt"
}

dependencies {

    compile(project(":unearth-core"))
    compile(project(":unearth-dto"))
    compile(project(":unearth-micrometer"))
    compile(project(":unearth-static"))

    compile("org.http4k:http4k-core:3.154.1")
    compile("org.http4k:http4k-contract:3.143.0")

    compile("org.http4k:http4k-format-jackson:3.154.1")
    compile("org.http4k:http4k-server-netty:3.154.1")

    compile("com.natpryce:konfig:1.6.10.0")

    compile("io.swagger.core.v3:swagger-core:2.0.8")
    runtime("org.webjars:swagger-ui:3.22.2")

    compile("io.micrometer:micrometer-registry-jmx:1.1.4")

    compile("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.9.9")
    compile("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.9.9")

    runtime("ch.qos.logback:logback-classic:1.3.0-alpha4")

    testCompile("org.http4k:http4k-client-apache:3.154.1")
    testCompile("junit:junit:4.12")
}
