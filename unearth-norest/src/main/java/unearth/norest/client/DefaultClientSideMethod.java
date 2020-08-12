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

import unearth.norest.common.AbstractProcessedMethod;
import unearth.norest.common.RequestMethod;
import unearth.norest.common.Transformers;

public final class DefaultClientSideMethod extends AbstractProcessedMethod implements ClientSideMethod {
    
    public DefaultClientSideMethod(Method method, Transformers transformers) {
        super(method, transformers);
    }
    
    @Override
    public RequestMethod getRequestMethod() {
        return requestMethod();
    }
    
    @Override
    public String getContentType() {
        return stringBody() ? TEXT : JSON;
    }
    
    @Override
    public boolean isStringBody() {
        return super.stringBody();
    }
    
    @Override
    public Optional<Object> bodyArgument(Object... args) {
        return requestMethod().isEntity()
            ? Optional.ofNullable(args[bodyArgumentIndex()])
            : Optional.empty();
    }
    
    @Override
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
    
    @Override
    public boolean isReturnData() {
        return !nullReturn();
    }
    
    @Override
    public boolean isOptionalReturn() {
        return super.optionalReturn();
    }
    
    @Override
    public Class<?> getReturnType() {
        return super.returnType();
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
    
    private static final String JSON = "application/json;charset=UTF-8";
    
    private static final String TEXT = "text/plain;charset=UTF-8";
    
    private static final String PAR = "{}";
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + requestMethod() + " " + path() + (
            queryParameters().isEmpty() ? "" : "?" + String.join("&", queryParameters().values())
        ) + "]";
    }
}
