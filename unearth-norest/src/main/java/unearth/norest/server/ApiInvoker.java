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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import unearth.norest.common.Request;
import unearth.norest.common.Transformer;

public final class ApiInvoker<A> {
    
    private final ServerSideMethods<A> serverSideMethods;
    
    private final A impl;
    
    public ApiInvoker(Class<A> type, A impl, List<Transformer<?>> transformers) {
        this.impl = Objects.requireNonNull(impl, "impl");
        this.serverSideMethods = new ServerSideMethods<A>(type, transformers);
    }
    
    public Optional<Object> response(Request request) {
        return Optional.ofNullable(request)
            .flatMap(serverSideMethods::invocation)
            .map(invocation -> {
                try {
                    return invocation.apply(impl);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to invoke on " + impl + ": " + request, e);
                }
            });
    }
}