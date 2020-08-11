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
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import unearth.norest.common.ProcessedMethod;
import unearth.norest.common.Request;
import unearth.norest.common.Transformer;
import unearth.norest.common.Transformers;

public final class ForwardableMethods<A> {
    
    private final Transformers transformers;
    
    private final List<ForwardableMethod> forwardableMethods;
    
    public ForwardableMethods(Class<A> api, List<Transformer<?>> transformers) {
        this(api, new Transformers(transformers));
    }
    
    public ForwardableMethods(Class<A> api, Transformers transformers) {
        Objects.requireNonNull(api, "api");
        this.transformers = transformers;
        this.forwardableMethods = Arrays.stream(api.getMethods())
            .map(this::processed)
            .collect(Collectors.toList());
    }
    
    public Stream<Function<Object, Object>> invocation(Request request) {
        return forwardableMethods.stream()
            .flatMap(forwardableMethod ->
                forwardableMethod.matchingInvoker(request));
    }
    
    private ProcessedMethod processed(java.lang.reflect.Method method) {
        try {
            return new ProcessedMethod(method, this.transformers);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to process: " + method, e);
        }
    }
    
}

