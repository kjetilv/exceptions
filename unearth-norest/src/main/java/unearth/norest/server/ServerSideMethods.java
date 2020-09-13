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
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import unearth.norest.Transformer;
import unearth.norest.Transformers;
import unearth.norest.common.Request;

final class ServerSideMethods<A> {

    private final Transformers transformers;

    private final List<ServerSideMethod> serverSideMethods;

    ServerSideMethods(Class<A> api, List<Transformer<?>> transformers) {
        this(api, new Transformers(transformers));
    }

    ServerSideMethods(Class<A> api, Transformers transformers) {
        Objects.requireNonNull(api, "api");
        this.transformers = transformers;
        this.serverSideMethods = Arrays.stream(api.getMethods())
            .map(this::processed)
            .collect(Collectors.toList());
    }

    public Optional<Function<Object, Object>> invocation(Request request) {
        return serverSideMethods.stream()
            .map(serverSideMethod ->
                serverSideMethod.invocation(request))
            .flatMap(Optional::stream)
            .findFirst();
    }

    private ServerSideMethod processed(java.lang.reflect.Method method) {
        try {
            return new ServerSideMethod(method, this.transformers);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to process: " + method, e);
        }
    }
}

