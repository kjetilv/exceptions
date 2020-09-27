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

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import unearth.norest.IO;
import unearth.norest.Transformers;
import unearth.norest.common.AbstractProcessedMethod;
import unearth.norest.common.RequestMethod;

public final class ClientSideMethod extends AbstractProcessedMethod {

    public ClientSideMethod(Method method, Transformers transformers) {
        super(method, transformers);
    }

    public RequestMethod getRequestMethod() {
        return requestMethod();
    }

    public IO.ContentType getContentType() {
        return super.getContentType();
    }

    public boolean isStringBody() {
        return super.stringBody();
    }

    public Optional<Object> bodyArgument(Object... args) {
        return requestMethod().isEntity()
            ? Optional.ofNullable(args[bodyArgumentIndex()])
            : Optional.empty();
    }

    public String buildPath(Object... args) {
        if (args == null || args.length == 0 || requestMethod().isEntity()) {
            return path();
        }
        Object arg = args[0];
        String fullPath = toString(arg)
            .map(string -> path().replace(PAR, string))
            .orElseThrow(() ->
                new IllegalArgumentException("Not a recognized  path parameter: " + arg));
        String queryPath = queryPath(args);
        return queryPath == null || queryPath.isBlank()
            ? fullPath
            : fullPath + '?' + queryPath;
    }

    public boolean isReturnData() {
        return !nullReturn();
    }

    public boolean isOptionalReturn() {
        return super.optionalReturn();
    }

    public Class<?> getReturnType() {
        return super.returnType();
    }

    public Object wrapResponse(Object object) {
        if (object == null) {
            if (isOptionalReturn()) {
                return Optional.empty();
            }
            throw new IllegalStateException("No object returned");
        }
        return isOptionalReturn() ? Optional.of(object) : object;
    }

    private String queryPath(Object[] args) {
        Map<String, String> params = queryParameters().entrySet().stream()
            .filter(e -> args[e.getKey()] != null)
            .collect(Collectors.toMap(
                Map.Entry::getValue,
                e -> String.valueOf(args[e.getKey()])));
        return params.entrySet().stream()
            .map(e ->
                e.getKey() + '=' + e.getValue())
            .collect(Collectors.joining("&"));
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<String> toString(T arg) {
        return this.transformers().to((Class<T>) arg.getClass(), arg);
    }

    private static final String PAR = "{}";

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + requestMethod() + " " + path() + (
            queryParameters().isEmpty() ? "" : "?" + String.join("&", queryParameters().values())
        ) + "]";
    }
}
