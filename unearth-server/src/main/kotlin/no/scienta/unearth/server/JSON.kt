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
 *     along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
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
        .registerModule(forIds("api/v1")))

private fun forIds(prefix: String): Module =
        SimpleModule().apply {
            addSerializer(FaultId::class.java, serializer(prefix, "fault", true))
            addDeserializer(FaultId::class.java, deserializer(::FaultId))

            addSerializer(FaultTypeId::class.java, serializer(prefix, "fault-type", true))
            addDeserializer(FaultTypeId::class.java, deserializer(::FaultTypeId))

            addSerializer(FaultEventId::class.java, serializer(prefix, "fault-event", false))
            addDeserializer(FaultEventId::class.java, deserializer(::FaultEventId))

            addSerializer(CauseTypeId::class.java, serializer(prefix, "cause-type", false))
            addDeserializer(CauseTypeId::class.java, deserializer(::CauseTypeId))

            addSerializer(CauseId::class.java, serializer(prefix, "cause", false))
            addDeserializer(CauseId::class.java, deserializer(::CauseId))
}

private fun <T : Id> deserializer(id: (UUID) -> T): JsonDeserializer<T>? {
    return object : JsonDeserializer<T>() {
        override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): T {
            return p?.readValueAsTree<TreeNode>()?.let { tree ->
                tree.get("id")?.let { idNode ->
                    id(UUID.fromString((idNode as ValueNode).textValue()))
                }
            } ?: throw IllegalArgumentException("Could not parse Id")
        }
    }
}

private fun <T : Id> serializer(prefix: String, path: String, feed: Boolean = false): JsonSerializer<T>? {
    return object : JsonSerializer<T>() {
        override fun serialize(value: T?, gen: JsonGenerator?, serializers: SerializerProvider?) {
            gen?.apply {
                value?.let { id ->
                    writeStartObject()
                    writeStringField("id", id.hash.toString())
                    writeStringField("type", value.javaClass.simpleName)
                    writeStringField("link", "/$prefix/$path/${id.hash}")
                    if (feed) {
                        writeStringField("feed", "/$prefix/feed/$path/${id.hash}")
                    }
                    writeEndObject()
                }
            }
        }
    }
}
