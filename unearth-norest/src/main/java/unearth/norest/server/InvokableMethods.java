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

package unearth.norest.server;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import unearth.norest.common.ProcessedMethod;
import unearth.norest.common.Request;
import unearth.norest.common.Transformer;

public class InvokableMethods<A> {
    
    private final Map<Class<?>, Transformer<?>> transformers;
    
    private final List<ForwardableMethod> invokableMethods;
    
    public InvokableMethods(Class<A> api, Transformer<?>... transformers) {
        this(api, Arrays.asList(transformers));
    }
    
    public InvokableMethods(Class<A> api, List<Transformer<?>> transformers) {
        Objects.requireNonNull(api, "api");
        this.transformers = transformers == null || transformers.isEmpty()
            ? Collections.emptyMap()
            : transformers.stream()
                .collect(Collectors.toMap(Transformer::getType, e -> e));
        this.invokableMethods = Arrays.stream(api.getMethods())
            .map(this::processed)
            .collect(Collectors.toList());
    }
    
    private ProcessedMethod processed(java.lang.reflect.Method method) {
        try {
            return new ProcessedMethod(method, this.transformers);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to process: " + method, e);
        }
    }
    
    public Stream<ForwardableMethod.Invoke> invoke(Request request) {
        return invokableMethods.stream()
            .flatMap(invokableMethod ->
                invokableMethod.matching(request));
    }
}

