/*
 *     This file is part of Unearth.
 *
 *     Unearth is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Unearth is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Unearth.  If not, see <https://www.gnu.org/licenses/>.
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val jacksonVer = "2.9.9"

val http4kVer = "3.169.0"

val micrometerVer = "1.2.0"

val konfigVer = "1.6.10.0"

val swaggerVer = "2.0.8"

val logbackVer = "1.3.0-alpha4"

val cassandraVer = "3.1.4"

val metricsVer = "4.1.0"

val flywayVer = "6.0.0-beta2"

val slf4jVer = "1.8.0-beta4"

val junitVer = "4.12"

val swaggerUiVer = "3.23.0"

val kotlinVer = "1.3.40"

val testcontainersVer = "1.12.0"

plugins {
    kotlin("jvm") version "1.3.40"
    id("com.github.johnrengelman.shadow") version "5.1.0"
    maven
    `maven-publish`
}

allprojects {

    apply(plugin = "java")
    apply(plugin = "maven")
    apply(plugin = "maven-publish")

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    configure<JavaPluginConvention> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    group = "no.scienta.unearth"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        jcenter()
        google()
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

            compile("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVer")
            compile("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVer")
            compile("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVer")
            compile("com.fasterxml.jackson.core:jackson-databind:$jacksonVer")

            compile("io.swagger.core.v3:swagger-core:$swaggerVer")

            compile("com.datastax.cassandra:cassandra-driver-core:$cassandraVer")
            compile("com.datastax.cassandra:cassandra-driver-mapping:$cassandraVer")
            compile("com.datastax.cassandra:cassandra-driver-extras:$cassandraVer")

            compile("io.micrometer:micrometer-registry-jmx:$micrometerVer")
            compile("io.micrometer:micrometer-core:$micrometerVer")

            compile("org.slf4j:slf4j-api:$slf4jVer")

            compile("org.flywaydb:flyway-core:$flywayVer")
            compile("ch.qos.logback:logback-classic:$logbackVer")

            runtime("io.dropwizard.metrics:metrics-jmx:$metricsVer")
            runtime("io.dropwizard.metrics:metrics-core:$metricsVer")
            runtime("ch.qos.logback:logback-classic:$logbackVer")
            runtime("org.webjars:swagger-ui:$swaggerUiVer")

            testCompile("org.http4k:http4k-client-apache:$http4kVer")
            testCompile("junit:junit:$junitVer")

            testCompile("org.testcontainers:cassandra:$testcontainersVer")
        }
    }
}
