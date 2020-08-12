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

package unearth.norest.server;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import unearth.norest.common.AbstractProcessedMethod;
import unearth.norest.common.Request;
import unearth.norest.common.Transformers;

public final class DefaultServerSideMethod extends AbstractProcessedMethod implements ServerSideMethod {
    
    public DefaultServerSideMethod(Method method, Transformers transformers) {
        super(method, transformers);
    }
    
    @Override
    public Stream<Function<Object, Object>> invocation(Request request) {
        if (request.getMethod() != this.getHttpMethod()) {
            return Stream.empty();
        }
        String requestedPath = request.getPath();
        String path = normalized(requestedPath);
        if (!path.startsWith(getRootPath())) {
            return Stream.empty();
        }
        if (path.equals(getRootPath())) {
            return Stream.of(invoker(request, null));
        }
        int queryIndex = request.getQueryIndex();
        if (queryIndex < 0) {
            return matching(request, requestedPath);
        }
        return matching(request, requestedPath.substring(0, queryIndex));
    }
    
    private Stream<Function<Object, Object>> matching(Request request, String requestedPath) {
        Matcher matcher = getMatchPattern().matcher(requestedPath);
        if (matcher.matches()) {
            return Stream.of(invoker(request, matcher));
        }
        return Stream.empty();
    }
    
    private Function<Object, Object> invoker(Request request, Matcher matcher) {
        return impl -> {
            try {
                return invoke(request, matcher, impl);
            } catch (Exception e) {
                throw new IllegalArgumentException(this + " failed to invoke " + request, e);
            }
        };
    }
    
    private Object invoke(Request request, Matcher matcher, Object impl) {
        Map<String, String> queryParams = request.getQueryParameters();
        Map<String, Optional<?>> queryArgs = IntStream.range(0, getParameters().length)
            .boxed()
            .collect(Collectors.toMap(
                i -> getParameterNames()[i],
                i ->
                    getTransformers().from(getParameterTypes()[i], queryParams.get(getParameterNames()[i]))));
        List<String> pathParams = matches(matcher);
        if (pathParams.size() != getPathParameters().size()) {
            throw new IllegalArgumentException(this + " got bad path: " + request);
        }
        Map<String, Optional<?>> pathArgs = getPathParameters().entrySet().stream()
            .collect(Collectors.toMap(
                e -> getParameterNames()[e.getKey()],
                e ->
                    getTransformers().from(getParameterTypes()[e.getKey()], pathParams.get(e.getKey()))));
        Object[] args = Arrays.stream(getParameterNames())
            .map(name -> lookup(name, queryArgs, pathArgs))
            .map(opt -> opt.orElse(null))
            .toArray(Object[]::new);
        if (getHttpMethod().isEntity()) {
            String entity = request.getEntity();
            args[getBodyArgumentIndex()] = isStringBody()
                ? entity
                : getTransformers().from(getParameterTypes()[getBodyArgumentIndex()], entity);
        }
        Object result;
        try {
            result = getMethod().invoke(impl, args);
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to invoke on " + impl + ": " + getMethod() + "" + Arrays.toString(args), e);
        }
        if (result == null || !isReturnsData()) {
            return null;
        }
        if (isOptionalReturn()) {
            return ((Optional<?>) result).orElse(null);
        }
        return result;
    }
    
    private static String normalized(String annotatedPath) {
        return "/" + unpreslashed(unpostslashed(annotatedPath.trim()));
    }
    
    private static List<String> matches(Matcher matcher) {
        if (matcher == null) {
            return Collections.emptyList();
        }
        
        int groupCount = matcher.groupCount();
        if (groupCount == 0) {
            return Collections.emptyList();
        }
        
        return MatchSpliterator.stream(matcher)
            .collect(Collectors.toList());
    }
    
    @SafeVarargs
    private static Optional<?> lookup(String name, Map<String, Optional<?>>... maps) {
        return Arrays.stream(maps)
            .map(map -> map.getOrDefault(name, Optional.empty()))
            .flatMap(Optional::stream)
            .findFirst();
    }
    
    private static String unpreslashed(String path) {
        return path.startsWith("/") ? unpreslashed(path.substring(1)) : path;
    }
    
    private static String unpostslashed(String path) {
        return path.endsWith("/") ? unpostslashed(path.substring(0, path.length() - 1)) : path;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getHttpMethod() + " " + getPath() + (
            getQueryParameters().isEmpty() ? "" : "?" + String.join("&", getQueryParameters().values())
        ) + "]";
    }
}
