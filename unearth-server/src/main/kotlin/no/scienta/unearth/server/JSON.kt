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

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ValueNode
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.scienta.unearth.munch.id.*
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings
import java.util.*

object JSON : ConfigurableJackson(KotlinModule()
        .asConfigurable()
        .withStandardMappings()
        .done()
        .disableDefaultTyping()
        .setDateFormat(StdDateFormat())
        .setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY)
        .configure(FAIL_ON_UNKNOWN_PROPERTIES, true)
        .configure(FAIL_ON_IGNORED_PROPERTIES, true)
        .registerModule(Jdk8Module())
        .registerModule(JavaTimeModule())
        .registerModule(forIds(Unearth.conf("server.api"))))

private fun forIds(prefix: String): Module =
        SimpleModule().apply {
            addSerializer(FaultId::class.java, serializer(prefix, "fault", true))
            addDeserializer(FaultId::class.java, deserializer(::FaultId))

            addSerializer(FaultStrandId::class.java, serializer(prefix, "fault-strand", true))
            addDeserializer(FaultStrandId::class.java, deserializer(::FaultStrandId))

            addSerializer(FaultEventId::class.java, serializer(prefix, "fault-event", false))
            addDeserializer(FaultEventId::class.java, deserializer(::FaultEventId))

            addSerializer(CauseStrandId::class.java, serializer(prefix, "cause-strand", false))
            addDeserializer(CauseStrandId::class.java, deserializer(::CauseStrandId))

            addSerializer(CauseId::class.java, serializer(prefix, "cause", false))
            addDeserializer(CauseId::class.java, deserializer(::CauseId))
        }

private fun <T : Id> deserializer(toId: (UUID) -> T): JsonDeserializer<T>? {
    return object : JsonDeserializer<T>() {
        override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): T {
            return p?.readValueAsTree<TreeNode>()?.let { tree ->
                tree.get("id")?.let { idNode ->
                    toId(UUID.fromString((idNode as ValueNode).textValue()))
                }
            } ?: throw IllegalArgumentException("Could not parse Id")
        }
    }
}

private fun <T : Id> serializer(prefix: String, path: String, feed: Boolean = false): JsonSerializer<T>? {
    return object : JsonSerializer<T>() {
        override fun serialize(value: T?, gen: JsonGenerator?, serializers: SerializerProvider?) {
            gen?.let { generator ->
                value?.let { id ->
                    obj(generator, refs(id, value))
                }
            }
        }

        private fun refs(id: T, value: T): Map<String, String> =
                if (feed)
                    base(id, value).plus("feed" to "$prefix/feed/$path/${id.hash}")
                else
                    base(id, value)


        private fun base(id: T, value: T): Map<String, String> = mapOf(
                "id" to id.hash.toString(),
                "type" to value.javaClass.simpleName,
                "link" to "$prefix/$path/${id.hash}")
    }
}

private fun obj(gen: JsonGenerator, map: Map<String, String>) {
    gen.writeStartObject()
    map.forEach { (field, value) ->
        gen.writeStringField(field, value)
    }
    gen.writeEndObject()
}
