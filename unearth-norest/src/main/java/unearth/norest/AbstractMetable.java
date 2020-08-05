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

package unearth.norest;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractMetable<M extends AbstractMetable<M>> {
    
    private final ObjectMapper objectMapper;
    
    private final Map<Class<?>, Transformer<?>> transformers;
    
    private final Map<Method, Meta> metas = new HashMap<>();
    
    protected AbstractMetable(ObjectMapper objectMapper, List<Transformer<?>> transformers) {
        this.objectMapper = objectMapper;
        this.transformers = transformers == null || transformers.isEmpty()
            ? Collections.emptyMap()
            : transformers.stream()
                .collect(Collectors.toMap(Transformer::getType, e -> e));
    }
    
    public final <T> M transform(Class<T> type, Function<T, String> fun) {
        return transform(Transformer.get(type, fun));
    }
    
    public final M transform(Transformer<?>... transformers) {
        return transform(Arrays.asList(transformers));
    }
    
    @SuppressWarnings("unchecked")
    public final M transform(List<Transformer<?>> transformers) {
        if (transformers == null || transformers.isEmpty()) {
            return (M) this;
        }
        return with(Stream.concat(
            this.transformers.values().stream(),
            transformers.stream()
        ).collect(Collectors.toList()));
    }
    
    protected ObjectMapper getObjectMapper() {
        return objectMapper;
    }
    
    protected abstract M with(List<Transformer<?>> transformers);
    
    protected Meta meta(Method method) {
        return metas.computeIfAbsent(
            method,
            __ ->
                new DefaultMeta(method, transformers));
    }
    
    protected byte[] writeBytes(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write body: " + value, e);
        }
    }
    
    protected Object readBytes(Class<?> type, InputStream inputStream) {
        try {
            return objectMapper.readerFor(type).readValue(inputStream);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read response: " + type + " <= " + inputStream, e);
        }
    }
    
    protected static Object wrap(Meta meta, Object object) {
        if (object == null) {
            if (meta.isReturnOptional()) {
                return Optional.empty();
            }
            throw new IllegalStateException("No object returned");
        }
        return meta.isReturnOptional() ? Optional.of(object) : object;
    }
}
