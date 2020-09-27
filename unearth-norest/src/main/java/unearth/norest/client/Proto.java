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

package unearth.norest.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URI;

import unearth.norest.IO;
import unearth.norest.Transformers;

public final class Proto {

    public static <T> T type(Class<T> api, URI uri, IO io, Transformers transformers) {
        ClientSideMethods methods = new ClientSideMethods(transformers);
        InvocationHandler invocationHandler = new ClientInvocationHandler(api, uri, io, methods);
        return api.cast(Proxy.newProxyInstance(
            Thread.currentThread().getContextClassLoader(),
            new Class<?>[] { api },
            invocationHandler));
    }

    private Proto() {
    }
}
