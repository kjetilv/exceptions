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

import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unearth.norest.common.Request;
import unearth.norest.server.ForwardableMethods;

public class ApiInvoker<A> {
    
    private static final Logger log = LoggerFactory.getLogger(ApiInvoker.class);
    
    private final String prefix;
    
    private final A impl;
    
    private final ForwardableMethods<A> forwardableMethods;
    
    public ApiInvoker(String prefix, A impl, ForwardableMethods<A> forwardableMethods) {
        this.prefix = prefix == null || prefix.isBlank() ? null : prefix.trim();
        this.impl = Objects.requireNonNull(impl, "impl");
        this.forwardableMethods = forwardableMethods;
    }
    
    public Optional<Object> response(Request request) {
        if (prefix == null || request.getPath().startsWith(prefix)) {
            return forwardableMethods.invocation(request.suffix(prefix))
                .map(invoke ->
                    invoke.apply(impl))
                .findFirst();
        }
        return Optional.empty();
    }
}
