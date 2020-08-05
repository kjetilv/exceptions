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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

final class DefaultMeta implements Meta {
    
    private final boolean post;
    
    private final String path;
    
    private final Map<Integer, String> queryParameters;
    
    private final int pathParam;
    
    private final int bodyParam;
    
    private final boolean stringBody;
    
    private final Class<?> returnType;
    
    private final boolean optionalReturn;
    
    private final boolean returnsData;
    
    private final Map<? extends Class<?>, Transformer<?>> transformers;
    
    DefaultMeta(Method method, Map<Class<?>, Transformer<?>> transformers) {
        POST post = method.getAnnotation(POST.class);
        GET get = method.getAnnotation(GET.class);
        
        this.post = post != null;
        this.path = (this.post ? post.value()
            : get != null ? get.value()
                : "").trim();
        if (this.path.startsWith("/")) {
            throw new IllegalArgumentException("Got pre-slashed path in " + method + ": " + path);
        }
        if (this.path.endsWith("/")) {
            throw new IllegalArgumentException("Got post-slashed path in " + method + ": " + path);
        }
        
        Class<?> returnType = method.getReturnType();
        this.optionalReturn = Optional.class.isAssignableFrom(returnType);
        this.returnsData = returnType != void.class;
        this.returnType = getActualReturnType(method, this.optionalReturn, returnType);
        
        this.pathParam = this.post ? -1 : 0;
        this.bodyParam = this.post ? 0 : -1;
        this.stringBody = this.post && method.getParameterTypes()[this.bodyParam] == String.class;
        
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        
        this.queryParameters = IntStream.range(0, parameterAnnotations.length)
            .filter(i ->
                parameterAnnotations[i].length > 0)
            .boxed()
            .collect(Collectors.toMap(
                i -> i,
                i ->
                    annotatedName(parameterAnnotations[i])
                        .or(() -> reflectiveName(method, i))
                        .orElseThrow(() ->
                            new IllegalArgumentException(
                                "Could not extract argument name for #" + i + " for " + method))));
        this.transformers = transformers == null || transformers.isEmpty()
                    ? Collections.emptyMap()
                    : Collections.unmodifiableMap(transformers);
    }
    
    @Override
    public boolean post() {
        return post;
    }
    
    @Override
    public String contentType() {
        return stringBody ? TEXT : JSON;
    }
    
    @Override
    public boolean isStringBody() {
        return stringBody;
    }
    
    @Override
    public Optional<Object> bodyArgument(Object... args) {
        return bodyParam < 0
            ? Optional.empty()
            : Optional.ofNullable(args[bodyParam]);
    }
    
    @Override
    public String path(Object... args) {
        if (args == null || args.length == 0 || pathParam < 0) {
            return path;
        }
        Object arg = args[pathParam];
        String fullPath = path.replace(PAR, toString(arg));
        String queryPath = queryPath(args);
        return queryPath == null || queryPath.isBlank()
            ? fullPath
            : fullPath + '?' + queryPath;
    }
    
    @Override
    public boolean isReturnData() {
        return returnsData;
    }
    
    @Override
    public boolean isReturnOptional() {
        return optionalReturn;
    }
    
    @Override
    public Class<?> getReturnType() {
        return returnType;
    }
    
    private String queryPath(Object[] args) {
        Map<String, String> params = queryParameters.entrySet().stream()
            .filter(e -> args[e.getKey()] != null)
            .collect(Collectors.toMap(
                Map.Entry::getValue,
                e -> String.valueOf(args[e.getKey()])));
        return params.entrySet().stream()
            .map(e ->
                e.getKey() + '=' + e.getValue())
            .collect(Collectors.joining("&"));
    }
    
    private String toString(Object arg) {
        return transformer(arg).to(arg);
    }
    
    @SuppressWarnings("unchecked")
    private <T> Transformer<T> transformer(Object arg) {
        return (Transformer<T>) transformers.getOrDefault(arg.getClass(), new DefaultTransformer());
    }
    
    private static final String POST = "POST";
    
    private static final String GET = "GET";
    
    private static final String JSON = "application/json;charset=UTF-8";
    
    private static final String TEXT = "text/plain;charset=UTF-8";
    
    private static final String PAR = "{}";
    
    private static Optional<String> reflectiveName(Method method, Integer i) {
        return Optional.ofNullable(method.getParameters()[i])
            .filter(Parameter::isNamePresent)
            .map(Parameter::getName);
    }
    
    private static Optional<String> annotatedName(Annotation[] parameterAnnotation) {
        return Optional.of(((Q) parameterAnnotation[0]).value())
            .filter(s -> !s.isBlank());
    }
    
    private static Class<?> getActualReturnType(Method method, boolean optional, Class<?> nominalReturnType) {
        if (!optional) {
            return nominalReturnType;
        }
        Type genType = method.getGenericReturnType();
        if (!(genType instanceof ParameterizedType)) {
            throw new IllegalStateException("Could not resolve generic type of " + genType + ": " + method);
        }
        Type[] args = ((ParameterizedType) genType).getActualTypeArguments();
        if (!(args[0] instanceof Class<?>)) {
            throw new IllegalStateException("Could not resolve generic type of " + genType + ": " + method);
        }
        return (Class<?>) args[0];
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + (post ? POST : GET) + " " + path + (
            queryParameters.isEmpty() ? "" : "?" + String.join("&", queryParameters.values())
        ) + "]";
    }
}
