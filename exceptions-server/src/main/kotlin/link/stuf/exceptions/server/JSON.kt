package link.stuf.exceptions.server

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings

object JSON : ConfigurableJackson(KotlinModule()
        .asConfigurable()
        .withStandardMappings()
        .done()
        .disableDefaultTyping()
        .setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY)
        .configure(FAIL_ON_UNKNOWN_PROPERTIES, true)
        .configure(FAIL_ON_IGNORED_PROPERTIES, true)
        .registerModule(Jdk8Module())
        .registerModule(JavaTimeModule())
)
