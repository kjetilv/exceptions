plugins {
    kotlin("jvm")
}

dependencies {
    compile(project(":unearth-core"))
    compile(project(":unearth-metrics"))
    compile(project(":unearth-static"))
    compile(project(":unearth-turbo"))

    compile("org.http4k:http4k-core")
    compile("org.http4k:http4k-contract")
    testCompile("org.http4k:http4k-client-apache")

    compile("org.http4k:http4k-format-jackson")
    compile("org.http4k:http4k-server-netty")

    compile("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    compile("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    compile("io.swagger.core.v3:swagger-core")
    runtime("org.webjars:swagger-ui")

    compile("io.micrometer:micrometer-registry-jmx")

    compile("com.natpryce:konfig:1.6.10.0")

    runtime("ch.qos.logback:logback-classic")

    testCompile("junit:junit:4.12")

    implementation(kotlin("stdlib"))
}
