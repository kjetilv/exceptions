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
