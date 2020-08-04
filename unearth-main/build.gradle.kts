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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow")
    kotlin("jvm")
}

tasks.withType<ShadowJar>() {
    manifest {
        attributes(
                mapOf(Pair("Main-Class", "no.scienta.unearth.main.MainKt"))
        )
    }
    mergeServiceFiles()
}

dependencies {
    implementation(project(":unearth-core"))
    implementation(project(":unearth-util"))
    implementation(project(":unearth-statik"))
    implementation(project(":unearth-munch"))
    implementation(project(":unearth-jdbc"))
    implementation(project(":unearth-analysis"))
    implementation(project(":unearth-server"))

    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-contract")
    implementation("org.http4k:http4k-format-jackson")
    implementation("org.http4k:http4k-server-netty")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("ch.qos.logback:logback-classic")

    implementation(kotlin("stdlib"))

    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.webjars:swagger-ui")
    runtimeOnly("ch.qos.logback:logback-classic")
}
