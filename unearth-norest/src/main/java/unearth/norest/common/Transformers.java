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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Transformers {
    
    public static final Transformers EMPTY = new Transformers(Collections.emptyList());
    
    private final Map<Class<?>, Transformer<?>> transformers;
    
    public Transformers(List<Transformer<?>> transformers) {
        this.transformers = transformers == null || transformers.isEmpty()
            ? Collections.emptyMap()
            : allTransformers(transformers);
    }
    
    public <T> Optional<T> from(Class<T> type, String string) {
        try {
            return transformer(type).from(string);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not transform to " + type + ": " + string, e);
        }
    }
    
    public <T> Optional<String> to(Class<T> type, T t) {
        return transformer(type).to(t);
    }
    
    @SuppressWarnings("unchecked")
    private <T> Transformer<T> transformer(Class<T> type) {
        return (Transformer<T>) transformers.getOrDefault(type, new DefaultTransformer<>(type));
    }
    
    private static final Map<Class<?>, Transformer<?>> PRIMITIVES = Stream.of(
        
        Transformer.from(boolean.class, Boolean::parseBoolean, false),
        Transformer.from(float.class, Float::parseFloat, 0.0f),
        Transformer.from(double.class, Double::parseDouble, 0.0d),
        Transformer.from(long.class, Long::parseLong, 0L),
        Transformer.from(short.class, Short::parseShort, (short) 0),
        Transformer.from(byte.class, Byte::parseByte, (byte) 0),
        Transformer.from(int.class, Integer::parseInt, 0),
        Transformer.from(char.class, s -> s.charAt(0), (char) 0),
        
        Transformer.from(Boolean.class, Boolean::parseBoolean),
        Transformer.from(Float.class, Float::parseFloat),
        Transformer.from(Double.class, Double::parseDouble),
        Transformer.from(Long.class, Long::parseLong),
        Transformer.from(Short.class, Short::parseShort),
        Transformer.from(Byte.class, Byte::parseByte),
        Transformer.from(Integer.class, Integer::parseInt),
        Transformer.from(Character.class, s -> s.charAt(0))
    
    ).collect(Collectors.toMap(Transformer::getType, Function.identity()));
    
    private static Map<Class<?>, Transformer<?>> allTransformers(List<Transformer<?>> transformers) {
        Map<Class<?>, Transformer<?>> added = transformers == null || transformers.isEmpty()
            ? Collections.emptyMap()
            : transformers.stream()
                .collect(Collectors.toMap(Transformer::getType, e -> e));
        return Stream.of(
            PRIMITIVES.entrySet(),
            added.entrySet()
        ).flatMap(Set::stream).collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue));
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + transformers.size() + "]";
    }
}
