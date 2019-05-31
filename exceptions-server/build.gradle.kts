plugins {
    kotlin("jvm") version "1.3.31"
    application
}

application {
    mainClassName = "link/stuf/exceptions/server/MainKt"
}

dependencies {

    compile(project(":exceptions-core"))
    compile(project(":exceptions-micrometer"))

    compile("org.http4k:http4k-core:3.143.1")
    compile("org.http4k:http4k-server-netty:3.143.1")
    compile ("org.http4k:http4k-format-jackson:3.143.1")

    compile("org.http4k:http4k-client-apache:3.143.1")
    compile ("org.http4k:http4k-client-websocket:3.143.1")

    compile("io.swagger.core.v3:swagger-core:2.0.8")
    compile("io.micrometer:micrometer-registry-jmx:1.1.4")

    compile("com.fasterxml.jackson.module:jackson-module-parameter-names:2.9.8")
    compile("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.9.8")
    compile("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.9.8")

    runtime("org.webjars:swagger-ui:3.22.1")
    runtime("ch.qos.logback:logback-classic:1.3.0-alpha4")

    testCompile("junit:junit:4.12")
}
