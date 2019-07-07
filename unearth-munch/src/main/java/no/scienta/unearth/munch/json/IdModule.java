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

package no.scienta.unearth.munch.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ValueNode;
import no.scienta.unearth.munch.id.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public class IdModule extends SimpleModule {

    public static IdModule defaults() {
        return new IdModule()
            .add(FaultId.class, FaultId::new)
            .add(FaultStrandId.class, FaultStrandId::new)
            .add(FaultEventId.class, FaultEventId::new)
            .add(CauseId.class, CauseId::new)
            .add(CauseStrandId.class, CauseStrandId::new);
    }

    @SafeVarargs
    private final <T extends Id> IdModule add(
        Class<T> idClass,
        Function<UUID, T> toId,
        Function<Id, Map.Entry<String, String>>... fields
    ) {
        addDeserializer(idClass, deserializer(toId));
        addSerializer(idClass, serializer(fields));
        return this;
    }

    private static <T extends Id> JsonDeserializer<T> deserializer(Function<UUID, T> toId) {
        return new JsonDeserializer<T>() {
            @Override
            public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                TreeNode node = p.readValueAsTree();
                return Optional.ofNullable(node)
                    .map(treeNode -> treeNode.get("id"))
                    .filter(ValueNode.class::isInstance)
                    .map(ValueNode.class::cast)
                    .map(ValueNode::textValue)
                    .map(UUID::fromString)
                    .map(toId)
                    .orElseThrow(() ->
                        new IllegalArgumentException("Not a valid ID node: " + node));
            }
        };
    }

    @SafeVarargs
    private static <T extends Id> JsonSerializer<T> serializer(Function<Id, Map.Entry<String, String>>... fields) {
        return new JsonSerializer<T>() {
            @Override
            public void serialize(T value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
                gen.writeStartObject();
                gen.writeStringField("id", value.getHash().toString());
                Arrays.stream(fields).forEach(field -> {
                    Map.Entry<String, String> entry = field.apply(value);
                    try {
                        gen.writeStringField(entry.getKey(), entry.getValue());
                    } catch (IOException e) {
                        throw new IllegalStateException("Failed to write " + entry, e);
                    }
                });
                gen.writeEndObject();
            }
        };
    }
}
