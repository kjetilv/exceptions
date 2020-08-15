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

package unearth.norest.common;

import java.io.InputStream;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import unearth.norest.IOHandler;

public final class JacksonIOHandler implements IOHandler {
    
    public static final IOHandler DEFAULT = withDefaults(new ObjectMapper());
    
    public static IOHandler withDefaults(ObjectMapper objectMapper) {
        return new JacksonIOHandler(objectMapper
            .enable(SerializationFeature.INDENT_OUTPUT)
            .deactivateDefaultTyping()
            .setDateFormat(new StdDateFormat())
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
            .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, true)
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule()));
    }
    
    private final ObjectMapper objectMapper;
    
    public JacksonIOHandler(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }
    
    @Override
    public byte[] writeBytes(Object value) {
        if (value == null) {
            return NO_BYTES;
        }
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write body: " + value, e);
        }
    }
    
    @Override
    public Object readBytes(Class<?> type, InputStream inputStream) {
        try {
            return objectMapper.readerFor(type).readValue(inputStream);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read response: " + type + " <= " + inputStream, e);
        }
    }
    
    private static final byte[] NO_BYTES = new byte[0];
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + objectMapper + "]";
    }
}
