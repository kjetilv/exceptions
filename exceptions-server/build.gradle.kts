plugins {
    kotlin("jvm") version "1.3.31"
}

dependencies {

    compile(project(":exceptions-api"))
    compile(project(":exceptions-core"))

    compile("org.http4k:http4k-core:3.143.1")
    compile("org.http4k:http4k-server-netty:3.143.1")
    compile ("org.http4k:http4k-format-jackson:3.143.1")

    compile("org.http4k:http4k-client-apache:3.143.1")
    compile ("org.http4k:http4k-client-websocket:3.143.1")
    
    compile("io.swagger.core.v3:swagger-core:2.0.8")

    runtime("org.webjars:swagger-ui:3.22.1")

    testCompile("junit:junit:4.12")
}
