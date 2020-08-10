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

package unearth.server

import com.natpryce.konfig.*
import java.time.Duration

data class UnearthlyConfig(

    val prefix: String = "/api/v1",

    val host: String = "0.0.0.0",

    val port: Int = 8080,

    val connectTimeout: Duration = Duration.ofSeconds(30),

    val selfDiagnose: Boolean = false,

    val unearthlyLogging: Boolean = false,

    val unearthlyMemory: Boolean = true,

    val cassandra: UnearthlyCassandraConfig = UnearthlyCassandraConfig(),

    val db: UnearthlyDbConfig = UnearthlyDbConfig()
) {

    companion object {

        fun load(config: Configuration = loadConfiguration()): UnearthlyConfig =
            UnearthlyConfig(
                prefix = config[Key("server.api", stringType)],
                host = config[Key("server.host", stringType)],
                port = config[Key("server.port", intType)],
                selfDiagnose = config[Key("unearth.self-diagnose", booleanType)],
                unearthlyLogging = config[Key("unearth.logging", booleanType)],
                unearthlyMemory = config[Key("unearth.memory", booleanType)],
                cassandra = UnearthlyCassandraConfig(
                    host = config[Key("unearth.cassandra-host", stringType)],
                    port = config[Key("unearth.cassandra-port", intType)],
                    dc = config[Key("unearth.cassandra-dc", stringType)],
                    keyspace = config[Key("unearth.cassandra-keyspace", stringType)]
                ),
                db = UnearthlyDbConfig(
                    host = config[Key("unearth.db-host", stringType)],
                    port = config[Key("unearth.db-port", intType)],
                    username = config[Key("unearth.db-username", stringType)],
                    password = config[Key("unearth.db-password", stringType)],
                    schema = config[Key("unearth.db-schema", stringType)]
                )
            )

        private fun loadConfiguration(): Configuration {
            return ConfigurationProperties.systemProperties() overriding
                    EnvironmentVariables() overriding
                    ConfigurationProperties.fromResource("defaults.properties")
        }
    }

}

data class UnearthlyDbConfig(

        val host: String = "127.0.0.1",

        val username: String = "postgres",

        val password: String = "",

        val port: Int = 5432,

        val schema: String = "unearth",

        val jdbc: String = "jdbc:postgresql://127.0.0.1:5432/unearth"
)

data class UnearthlyCassandraConfig(

        val host: String = "127.0.0.1",

        val port: Int = 9042,

        val dc: String = "datacenter1",

        val keyspace: String = "unearth"
)
