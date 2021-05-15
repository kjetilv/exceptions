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

val jacksonVer = "2.12.0"
val micrometerVer = "1.7.0"
val konfigVer = "1.6.10.0"
val nettyVer = "4.1.56.Final"
val swaggerVer = "2.1.4"
val logbackVer = "1.2.3"
val cassandraDriverVer = "4.9.0"
val metricsVer = "4.1.0"
val flywayVer = "7.3.0"
val slf4jVer = "1.7.30"
val hikariVer = "4.0.3"
val postgresJdbcVer = "42.2.20"
val junitVer = "4.12"
val jupiterVer = "5.3.1"
val swaggerUiVer = "3.38.0"
val kotlinVer = "1.3.72"
val testcontainersVer = "1.15.1"
val hsqldbVer = "2.5.1"
val assertjVer = "3.13.2"
val shadowVer = "6.1.0"
val bytebuddyVer = "1.11.0"

plugins {
    kotlin("jvm") version "1.4.20"
    id("com.github.johnrengelman.shadow") version "6.1.0"
    `maven-publish`
}

allprojects {

    apply(plugin = "java")
    apply(plugin = "maven-publish")

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-parameters")
    }

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_16
        targetCompatibility = JavaVersion.VERSION_16
        modularity.inferModulePath.set(true)
        withSourcesJar()
    }

    group = "no.scienta.unearth"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        google()
    }

    dependencies {
        testImplementation("junit:junit")
    }

    configurations.all {
        resolutionStrategy {
            failOnVersionConflict()
            preferProjectModules()
            force(
                "io.netty:netty-handler:$nettyVer",
                "io.dropwizard.metrics:metrics-core:$metricsVer",
                "io.dropwizard.metrics:metrics-jmx:$metricsVer",
                "com.fasterxml.jackson.core:jackson-annotations:$jacksonVer",
                "net.bytebuddy:byte-buddy:$bytebuddyVer",
                "org.yaml:snakeyaml:1.27",
                "com.github.jnr:jffi:1.2.19",
                "com.github.jnr:jnr-ffi:2.1.10"
            )
        }
    }

    fun jackson(type: String, value: String) = "com.fasterxml.jackson.$type:jackson-$type-$value:$jacksonVer"
    fun cassandra(dep: String) = "com.datastax.oss:java-driver-$dep:$cassandraDriverVer"

    dependencies {
        constraints {
            implementation("io.netty:netty-all:$nettyVer")
            implementation("com.natpryce:konfig:$konfigVer")

            implementation(jackson("datatype", "jdk8"))
            implementation(jackson("datatype", "jsr310"))
            implementation(jackson("module", "kotlin"))

            implementation("net.bytebuddy:byte-buddy:$bytebuddyVer")

            implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVer")
            implementation("io.swagger.core.v3:swagger-core:$swaggerVer")

            implementation(cassandra("core"))
            implementation(cassandra("query-builder"))
            implementation(cassandra("mapper-runtime"))

            implementation("io.micrometer:micrometer-registry-jmx:$micrometerVer")
            implementation("io.micrometer:micrometer-registry-prometheus:$micrometerVer")
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

            runtimeOnly("org.hsqldb:hsqldb:$hsqldbVer")

            testImplementation("junit:junit:$junitVer")
            testImplementation("org.junit.jupiter:junit-jupiter-api:$jupiterVer")
            testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterVer")
            testImplementation("org.assertj:assertj-core:3.13.2:$assertjVer")
            testImplementation("org.testcontainers:cassandra:$testcontainersVer")
        }
    }
}
