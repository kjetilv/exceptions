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

import unearth.norest.IO;
import unearth.norest.Transformers;
import unearth.norest.common.AbstractProcessedMethod;
import unearth.norest.common.Request;

public abstract class ServerSideMethod extends AbstractProcessedMethod {

    public ServerSideMethod(Method method, Transformers transformers) {
        super(method, transformers);
    }

    public Function<Object, byte[]> invoker(IO io, Request req) {
        return invoker(io, req, matcher(req));
    }

    public boolean methodMatch(Request request) {
        return request.getMethod() == requestMethod();
    }

    public int pathMatch(Request req) {
        return req.getPath().startsWith(path()) ? path().length()
            : Integer.MIN_VALUE;
    }

    protected abstract Object call(Object impl, Object[] args);

    private Matcher matcher(Request request) {
        if (request.getPath().startsWith(rootPath())) {
            Matcher matcher = matchPattern().matcher(request.getPath());
            if (matcher.matches()) {
                return matcher;
            }
        }
        return null;
    }

    private Function<Object, byte[]> invoker(IO io, Request request, Matcher matcher) {
        return impl -> {
            try {
                return io.writeBytes(
                    getContentType(),
                    invoke(request, matcher, impl));
            } catch (Exception e) {
                throw new IllegalArgumentException(this + " failed to invoke " + request, e);
            }
        };
    }

    private Object invoke(Request request, Matcher matcher, Object impl) {
        Map<String, String> queryParams = request.getQueryParameters();
        Map<String, Optional<?>> queryArgs = queryArgs(queryParams);
        List<String> pathParams = matches(matcher);
        if (pathParams.size() != pathParameters().size()) {
            throw new IllegalArgumentException(
                this + " got bad path arity " + pathParams.size() +
                    ", required " + pathParameters().size() + ": " + request);
        }
        Object[] args = arguments(queryArgs, pathParams);
        setEntityParameter(request, args);
        Object result;
        try {
            result = call(impl, args);
        } catch (Exception e) {
            throw new IllegalStateException(this + ": Failed to invoke " + request, e);
        }
        return result == null || nullReturn() ? null
            : optionalReturn() ? toOptional(result)
                : result;
    }

    private void setEntityParameter(Request request, Object[] args) {
        if (requestMethod().isEntity()) {
            String entity = request.getEntity();
            args[bodyArgumentIndex()] = stringBody()
                ? entity
                : transformers().from(parameterTypes()[bodyArgumentIndex()], entity);
        }
    }

    private Object[] arguments(Map<String, Optional<?>> queryArgs, List<String> pathParams) {
        Map<String, Optional<?>> pathArgs = pathParameters().entrySet().stream()
            .collect(Collectors.toMap(
                e -> parameterNames()[e.getKey()],
                e ->
                    transformers().from(parameterTypes()[e.getKey()], pathParams.get(e.getKey()))));
        return Arrays.stream(parameterNames())
            .map(name ->
                lookup(name, queryArgs, pathArgs))
            .map(opt ->
                opt.orElse(null))
            .toArray(Object[]::new);
    }

    private Map<String, Optional<?>> queryArgs(Map<String, String> queryParams) {
        return IntStream.range(0, parameters().length)
            .boxed()
            .collect(Collectors.toMap(
                i -> parameterNames()[i],
                i ->
                    transformers().from(parameterTypes()[i], queryParams.get(parameterNames()[i]))));
    }

    private static Object toOptional(Object result) {
        if (result instanceof Optional<?>) {
            return ((Optional<?>) result).orElse(null);
        }
        throw new IllegalStateException("Not an optional: " + result);
    }

    private static List<String> matches(Matcher matcher) {
        if (matcher == null) {
            return Collections.emptyList();
        }
        int groupCount = matcher.groupCount();
        if (groupCount == 0) {
            return Collections.emptyList();
        }
        return MatchSpliterator.stream(matcher).collect(Collectors.toList());
    }

    @SafeVarargs
    private static Optional<?> lookup(String name, Map<String, Optional<?>>... maps) {
        return Arrays.stream(maps)
            .map(map -> map.getOrDefault(name, Optional.empty()))
            .flatMap(Optional::stream)
            .findFirst();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + requestMethod() + " " + path() + (
            queryParameters().isEmpty() ? "" : "?" + String.join("&", queryParameters().values())
        ) + "]";
    }
}
