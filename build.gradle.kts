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

val jacksonVer = "2.11.0"
val http4kVer = "3.254.0"
val micrometerVer = "1.5.3"
val konfigVer = "1.6.10.0"
val swaggerVer = "2.0.8"
val logbackVer = "1.2.3"
val cassandraDriverVer = "4.1.0"
val metricsVer = "4.1.0"
val flywayVer = "6.0.0-beta2"
val slf4jVer = "1.7.30"
val hikariVer = "2.7.8"
val postgresJdbcVer = "42.2.6.jre7"
val junitVer = "4.12"
val swaggerUiVer = "3.23.0"
val kotlinVer = "1.3.72"
val testcontainersVer = "1.12.0"
val hsqldbVer = "2.5.0"
val assertjVer = "3.13.2"
val shadowVer = "6.0.0"

plugins {
    kotlin("jvm") version "1.3.72"
    id("com.github.johnrengelman.shadow") version "6.0.0"
    `maven-publish`
}

allprojects {

    apply(plugin = "java")
    apply(plugin = "maven-publish")

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    configure<JavaPluginConvention> {
        sourceCompatibility = JavaVersion.VERSION_14
        targetCompatibility = JavaVersion.VERSION_14
    }

    group = "no.scienta.unearth"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        jcenter()
        google()
    }

    dependencies {
        testImplementation("junit:junit")
    }

    fun http4k(dep: String) = "org.http4k:http4k-$dep:$http4kVer"
    fun jackson(type: String, value: String) = "com.fasterxml.jackson.$type:jackson-$type-$value:$jacksonVer"
    fun cassandra(dep: String) = "com.datastax.oss:java-driver-$dep:$cassandraDriverVer"

    dependencies {
        constraints {
            implementation(http4k("core"))
            implementation(http4k("contract"))
            implementation(http4k("format-jackson"))
            implementation(http4k("server-netty"))

            implementation("com.natpryce:konfig:$konfigVer")

            implementation(jackson("datatype", "jdk8"))
            implementation(jackson("datatype", "jsr310"))
            implementation(jackson("module", "kotlin"))

            implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVer")
            implementation("io.swagger.core.v3:swagger-core:$swaggerVer")

            implementation(cassandra("core"))
            implementation(cassandra("query-builder"))
            implementation(cassandra("mapper-runtime"))

            implementation("io.micrometer:micrometer-registry-jmx:$micrometerVer")
            implementation("io.micrometer:micrometer-core:$micrometerVer")

            implementation("org.slf4j:slf4j-api:$slf4jVer")

            implementation("com.zaxxer:HikariCP:$hikariVer")
            runtimeOnly("org.postgresql:postgresql:$postgresJdbcVer")

            implementation("org.flywaydb:flyway-core:$flywayVer")
            implementation("ch.qos.logback:logback-classic:$logbackVer")

            runtimeOnly("io.dropwizard.metrics:metrics-jmx:$metricsVer")
            runtimeOnly("io.dropwizard.metrics:metrics-core:$metricsVer")
            runtimeOnly("ch.qos.logback:logback-classic:$logbackVer")
            runtimeOnly("org.webjars:swagger-ui:$swaggerUiVer")

            testImplementation("org.http4k:http4k-client-apache:$http4kVer")
            testImplementation("junit:junit:$junitVer")

            testImplementation("org.assertj:assertj-core:3.13.2:$assertjVer")
            testImplementation("org.testcontainers:cassandra:$testcontainersVer")

            testRuntimeOnly("org.hsqldb:hsqldb:$hsqldbVer")
        }
    }
}
