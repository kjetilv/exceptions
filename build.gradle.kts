val jacksonVer = "2.9.9"

val http4kVer = "3.162.0"

val retrofitVer = "2.6.0"

val micrometerVer = "1.1.4"

val konfigVer = "1.6.10.0"

val swaggerVer = "2.0.8"

val logbackVer = "1.3.0-alpha4"

val flywayVer = "6.0.0-beta2"

val slf4jVer = "1.8.0-beta4"

val junitVer = "4.12"

val swaggerUiVer = "3.23.0"

val javaVer = JavaVersion.VERSION_1_8

val kotlinVer = "1.3.40"

plugins {
    kotlin("jvm") version "1.3.40"
    id("com.github.johnrengelman.shadow") version "5.0.0"
    maven
    `maven-publish`
}

allprojects {

    apply(plugin = "java")
    apply(plugin = "maven")
    apply(plugin = "maven-publish")

    configure<JavaPluginConvention> {
        sourceCompatibility = javaVer
        targetCompatibility = javaVer
    }

    group = "no.scienta.unearth"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    dependencies {
        testCompile("junit:junit")
    }

    dependencies {
        constraints {
            compile("org.http4k:http4k-core:$http4kVer")
            compile("org.http4k:http4k-contract:$http4kVer")

            compile("org.http4k:http4k-format-jackson:$http4kVer")
            compile("org.http4k:http4k-server-netty:$http4kVer")

            compile("com.natpryce:konfig:$konfigVer")

            compile("com.squareup.retrofit2:retrofit:$retrofitVer")
            compile("com.squareup.retrofit2:converter-jackson:$retrofitVer")

            compile("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVer")
            compile("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVer")
            compile("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVer")
            compile("com.fasterxml.jackson.core:jackson-databind:$jacksonVer")

            compile("io.swagger.core.v3:swagger-core:$swaggerVer")
            runtime("org.webjars:swagger-ui:$swaggerUiVer")

            compile("io.micrometer:micrometer-registry-jmx:$micrometerVer")
            compile("io.micrometer:micrometer-core:$micrometerVer")

            compile("org.slf4j:slf4j-api:$slf4jVer")

            compile("org.flywaydb:flyway-core:$flywayVer")

            runtime("ch.qos.logback:logback-classic:$logbackVer")
            compile("ch.qos.logback:logback-classic:$logbackVer")

            testCompile("org.http4k:http4k-client-apache:$http4kVer")
            testCompile("junit:junit:$junitVer")
        }
    }
}
