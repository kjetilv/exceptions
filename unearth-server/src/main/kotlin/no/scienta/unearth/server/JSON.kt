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
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.scienta.unearth.munch.id.*
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings

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
            .registerModule(forIds("api/v1", SimpleModule())))

private fun forIds(prefix: String, simpleModule: SimpleModule): Module? {
    val mapOf = mapOf(
            FaultEventId::class.java to "fault-event",
            FaultTypeId::class.java to "fault-type",
            FaultId::class.java to "fault",
            CauseTypeId::class.java to "cause-type",
            CauseId::class.java to "cause"
    )
    mapOf.forEach { (type, path) ->
        simpleModule.addSerializer<Id>(type, serializer(prefix, path))
    }
    return simpleModule
}

private fun <T : Id> serializer(prefix: String, path: String): JsonSerializer<T>? {
    return object : JsonSerializer<T>() {
        override fun serialize(value: T?, gen: JsonGenerator?, serializers: SerializerProvider?) {
            gen?.let {
                value?.run {
                    it.writeStartObject()
                    it.writeStringField("id", "$hash")
                    it.writeStringField("type", value.javaClass.simpleName)
                    it.writeStringField("self", "/$prefix/$path/$hash")
                    it.writeEndObject()
                }
            }
        }
    }
}
