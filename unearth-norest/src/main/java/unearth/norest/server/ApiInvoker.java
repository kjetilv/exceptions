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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import unearth.norest.HandlerIO;
import unearth.norest.IO;
import unearth.norest.IOHandler;
import unearth.norest.Transformer;
import unearth.norest.Transformers;
import unearth.norest.common.Request;

public final class ApiInvoker<A> {

    private final ServerSideMethods<A> serverSideMethods;

    private final A impl;

    public ApiInvoker(
        Class<A> type,
        A impl,
        Map<IO.ContentType, IOHandler> handlers,
        List<Transformer<?>> transformers
    ) {
        this.impl = Objects.requireNonNull(impl, "impl");
        this.serverSideMethods = new ServerSideMethods<>(
            type,
            new HandlerIO(handlers),
            new Transformers(transformers));
    }

    public Optional<byte[]> response(Request request) {
        return serverSideMethods.invoker(request)
            .map(invocation -> {
                try {
                    return invocation.apply(impl);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to invoke on " + impl + ": " + request, e);
                }
            });
    }
}
