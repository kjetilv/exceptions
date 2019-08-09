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

package no.scienta.unearth.server

data class UnearthlyConfig(

        val prefix: String = "/api/v1",

        val host: String = "0.0.0.0",

        val port: Int = 8080,

        val selfDiagnose: Boolean = false,

        val unearthlyLogging: Boolean = false,

        val cassandra: UnearthlyCassandraConfig = UnearthlyCassandraConfig())

data class UnearthlyCassandraConfig(

    val host: String = "127.0.0.1",

    val port: Int = 9042,

    val dc: String = "datacenter1"
)
