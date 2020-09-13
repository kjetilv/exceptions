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

plugins {
    kotlin("jvm")
}

dependencies {
    testImplementation(project(":unearth-util"))
    testImplementation(project(":unearth-api"))
    testImplementation(project(":unearth-core"))
    testImplementation(project(":unearth-jdbc"))
    testImplementation(project(":unearth-storage"))
    testImplementation(project(":unearth-analysis"))
    testImplementation(project(":unearth-munch"))

    testImplementation(project(":unearth-server"))
    testImplementation(project(":unearth-client"))
    testImplementation(project(":unearth-norest"))

    testImplementation(project(":unearth-netty"))
    testImplementation(project(":unearth-http4k"))

    testImplementation("org.testcontainers:cassandra")

    testRuntimeOnly("org.postgresql:postgresql")
    testRuntimeOnly("org.webjars:swagger-ui")
    testRuntimeOnly("io.dropwizard.metrics:metrics-jmx")
    testRuntimeOnly("ch.qos.logback:logback-classic")

    testRuntimeOnly(kotlin("stdlib"))
}
