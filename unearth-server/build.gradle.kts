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
    implementation(project(":unearth-util"))
    implementation(project(":unearth-core"))
    implementation(project(":unearth-analysis"))
    implementation(project(":unearth-munch"))
    implementation(project(":unearth-memory"))
    implementation(project(":unearth-jdbc"))
    implementation(project(":unearth-statik"))

    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-contract")
    implementation("org.http4k:http4k-format-jackson")
    implementation("org.http4k:http4k-server-netty")

    implementation("ch.qos.logback:logback-classic")

    implementation("com.zaxxer:HikariCP")


    implementation("io.swagger.core.v3:swagger-core")
    implementation("io.micrometer:micrometer-registry-jmx")

    implementation("com.natpryce:konfig")

    testImplementation("org.http4k:http4k-client-apache")
    testImplementation("junit:junit:4.12")

    implementation(kotlin("stdlib"))
}
