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

package no.scienta.unearth.client.proto;

import no.scienta.unearth.client.dto.IdDto;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

final class MethodMeta {

    private static final byte[] EMPTY = new byte[0];

    private static final String POST = "POST";

    private static final String GET = "GET";

    private final boolean post;

    private final String path;

    private final Map<Integer, String> queries;

    private final int pathParam;

    private final int bodyParam;

    private final boolean stringBody;

    private final Function<Object, byte[]> writer;

    private final BiFunction<Class<?>, InputStream, Object> reader;

    private final Class<?> returnType;

    MethodMeta(Method method, Function<Object, byte[]> writer, BiFunction<Class<?>, InputStream, Object> reader) {
        POST post = method.getAnnotation(POST.class);
        GET get = method.getAnnotation(GET.class);

        this.post = post != null;
        this.path = post != null ? post.value()
            : get != null ? get.value()
            : "";
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Class<?>[] parameterTypes = method.getParameterTypes();

        int pathOrBody = IntStream.range(0, parameterAnnotations.length)
            .filter(i -> parameterAnnotations[i].length == 0)
            .findFirst()
            .orElse(-1);
        this.returnType = method.getReturnType();
        this.pathParam = this.post ? -1 : pathOrBody;
        this.bodyParam = this.post ? pathOrBody : -1;
        this.stringBody = this.post && parameterTypes[this.bodyParam] == String.class;
        this.queries = IntStream.range(0, parameterAnnotations.length)
            .filter(i ->
                parameterAnnotations[i].length > 0)
            .boxed()
            .collect(Collectors.toMap(
                i -> i,
                i -> ((Q) parameterAnnotations[i][0]).value()
            ));
        this.writer = writer;
        this.reader = reader;
    }

    boolean post() {
        return post;
    }

    byte[] body(Object[] args) {
        if (bodyParam < 0) {
            return EMPTY;
        }
        Object arg = args[bodyParam];
        if (stringBody) {
            return arg == null ? EMPTY : bytes(arg.toString());
        }
        return writer.apply(arg);
    }

    String path(Object[] args) {
        if (pathParam < 0) {
            return path;
        }
        Object arg = args[pathParam];
        String fullPath = path.replace("{}", arg instanceof IdDto
            ? ((IdDto) arg).uuid.toString()
            : arg.toString());
        Map<String, String> params = queries.entrySet().stream()
            .filter(e -> args[e.getKey()] != null)
            .collect(Collectors.toMap(
                Map.Entry::getValue,
                e ->
                    String.valueOf(args[e.getKey()])));
        String queryPath = params.entrySet().stream().map(e ->
            e.getKey() + '=' + e.getValue())
            .collect(Collectors.joining("&"));
        return queryPath.isEmpty() ? fullPath : fullPath + '?' + queryPath;
    }

    Object res(InputStream inputStream) {
        return returnType.cast(reader.apply(returnType, inputStream));
    }

    private byte[] stringWriter(Object string) {
        return string == null
            ? MethodMeta.EMPTY
            : bytes(string);
    }

    private byte[] bytes(Object string) {
        return string.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + (post ? POST : GET) + " " + path + (
            queries.isEmpty() ? "" : "?" + String.join("&", queries.values())
        ) + "]";
    }
}
