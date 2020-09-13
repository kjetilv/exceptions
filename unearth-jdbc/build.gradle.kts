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

dependencies {
    implementation(project(":unearth-util"))
    implementation(project(":unearth-hashable"))
    implementation(project(":unearth-core"))
    implementation(project(":unearth-metrics"))

    implementation("org.flywaydb:flyway-core")
    implementation("org.slf4j:slf4j-api")
    implementation("io.micrometer:micrometer-core")

    testImplementation(project(":unearth-memory"))
    testImplementation("com.zaxxer:HikariCP")
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("org.hsqldb:hsqldb")
}

