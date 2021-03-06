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

package unearth.norest.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import unearth.norest.IO;
import unearth.norest.Transformers;
import unearth.norest.annotations.DELETE;
import unearth.norest.annotations.GET;
import unearth.norest.annotations.HEAD;
import unearth.norest.annotations.POST;
import unearth.norest.annotations.PUT;
import unearth.norest.annotations.Q;

import static unearth.norest.IO.ContentType.APPLICATION_JSON;
import static unearth.norest.IO.ContentType.TEXT_PLAIN;

public abstract class AbstractProcessedMethod {

    private final boolean returnData;

    private final RequestMethod requestMethod;

    private final String path;

    private final String rootPath;

    private final Map<Integer, String> queryParameters;

    private final Map<Integer, String> pathParameters;

    private final boolean stringBody;

    private final Class<?> returnType;

    private final boolean optionalReturn;

    private final Pattern matchPattern;

    private final Transformers transformers;

    private final Method method;

    private final Class<?>[] parameterTypes;

    private final Parameter[] parameters;

    private final Annotation[][] parameterAnnotations;

    private final String[] parameterNames;

    private final int bodyArgumentIndex;

    private final IO.ContentType contentType;

    protected AbstractProcessedMethod(Method method, Transformers transformers) {
        this.method = Objects.requireNonNull(method, "method");

        Annotation annotation = getAnnotation(this.method);
        String annotatedPath = path(annotation);
        this.path = normalized(annotatedPath);

        int rootIndex = rootIndex(this.path);
        this.rootPath = rootIndex < 0 ? this.path : this.path.substring(0, rootIndex);
        this.requestMethod = httpMethod(annotation);

        String regex = PATH_ARG.matcher(this.path).replaceAll("\\([^/]*\\)");
        this.matchPattern = Pattern.compile(regex);

        Class<?> returnType = this.method.getReturnType();
        this.parameterTypes = this.method.getParameterTypes();
        this.parameters = this.method.getParameters();
        this.parameterAnnotations = this.method.getParameterAnnotations();

        this.optionalReturn = Optional.class.isAssignableFrom(returnType);
        this.returnData = returnType != void.class;
        this.returnType = getActualReturnType(this.method, this.optionalReturn, returnType);

        this.stringBody = this.requestMethod.isEntity() && parameterTypes[0] == String.class;
        this.bodyArgumentIndex = this.requestMethod.isEntity() ?
            IntStream.range(0, parameterAnnotations.length)
                .filter(i -> parameterAnnotations[i] == null || parameterAnnotations[i].length == 0)
                .findFirst()
                .orElseThrow(() ->
                    new IllegalStateException("No body argument could be derived for " + method)) :
            -1;

        this.contentType = Optional.ofNullable(method.getAnnotation(unearth.norest.annotations.ContentType.class))
            .map(unearth.norest.annotations.ContentType::value)
            .orElse(stringBody
                ? TEXT_PLAIN
                : APPLICATION_JSON);

        this.parameterNames = IntStream.range(0, parameters.length)
            .mapToObj(index ->
                index == bodyArgumentIndex
                    ? ""
                    : annotatedName(parameterAnnotations[index])
                        .or(() ->
                            reflectiveName(parameters[index]))
                        .orElseThrow(() ->
                            new IllegalArgumentException(
                                "Could not extract argument name #" + index + ": " + parameters[index])))
            .toArray(String[]::new);
        this.queryParameters = paramsWhere(i1 ->
            parameterAnnotations[i1].length > 0, this.parameterNames);
        this.pathParameters = requestMethod.isEntity()
            ? Collections.emptyMap()
            : paramsWhere(i ->
                parameterAnnotations[i] == null || parameterAnnotations[i].length == 0, this.parameterNames);
        this.transformers = transformers == null ? Transformers.EMPTY : transformers;
    }

    public IO.ContentType getContentType() {
        return contentType;
    }

    protected boolean nullReturn() {
        return !returnData;
    }

    protected RequestMethod requestMethod() {
        return requestMethod;
    }

    protected String path() {
        return path;
    }

    protected String rootPath() {
        return rootPath;
    }

    protected Map<Integer, String> queryParameters() {
        return queryParameters;
    }

    protected Map<Integer, String> pathParameters() {
        return pathParameters;
    }

    protected boolean stringBody() {
        return stringBody;
    }

    protected Class<?> returnType() {
        return returnType;
    }

    protected boolean optionalReturn() {
        return optionalReturn;
    }

    protected Pattern matchPattern() {
        return matchPattern;
    }

    protected Transformers transformers() {
        return transformers;
    }

    protected Method method() {
        return method;
    }

    protected Class<?>[] parameterTypes() {
        return parameterTypes;
    }

    protected Parameter[] parameters() {
        return parameters;
    }

    protected String[] parameterNames() {
        return parameterNames;
    }

    protected int bodyArgumentIndex() {
        return bodyArgumentIndex;
    }

    private static final Pattern PATH_ARG = Pattern.compile("\\{\\s*}");

    private static Optional<String> reflectiveName(Parameter parameter) {
        return parameter.isNamePresent()
            ? Optional.of(parameter.getName())
            : Optional.empty();
    }

    private static Map<Integer, String> paramsWhere(IntPredicate intPredicate, String[] parameterNames) {
        return IntStream.range(0, parameterNames.length)
            .filter(intPredicate)
            .boxed()
            .collect(Collectors.toMap(i -> i, i -> parameterNames[i]));
    }

    private static int rootIndex(String path) {
        return IntStream.of(path.indexOf('?'), path.indexOf('{'))
            .filter(i -> i > 0)
            .min()
            .orElse(-1);
    }

    private static Annotation getAnnotation(Method method) {
        return List.of(
            GET.class,
            POST.class,
            PUT.class,
            DELETE.class,
            HEAD.class
        ).stream()
            .flatMap(anno ->
                Optional.ofNullable(method.getAnnotation(anno)).stream())
            .findFirst()
            .orElseThrow(() ->
                new IllegalArgumentException("Non-annotated method " + method));
    }

    private static String normalized(String annotatedPath) {
        return "/" + unpreslashed(unpostslashed(annotatedPath.trim()));
    }

    private static RequestMethod httpMethod(Annotation annotation) {
        if (annotation instanceof GET) {
            return RequestMethod.GET;
        }
        if (annotation instanceof POST) {
            return RequestMethod.POST;
        }
        if (annotation instanceof PUT) {
            return RequestMethod.PUT;
        }
        return RequestMethod.HEAD;
    }

    private static String unpreslashed(String path) {
        return path.startsWith("/") ? unpreslashed(path.substring(1)) : path;
    }

    private static String unpostslashed(String path) {
        return path.endsWith("/") ? unpostslashed(path.substring(0, path.length() - 1)) : path;
    }

    private static String path(Annotation annotation) {
        return annotation instanceof GET ? ((GET) annotation).value()
            : annotation instanceof POST ? ((POST) annotation).value()
                : annotation instanceof PUT ? ((PUT) annotation).value()
                    : annotation instanceof DELETE ? ((DELETE) annotation).value()
                        : ((HEAD) annotation).value();
    }

    private static Optional<String> annotatedName(Annotation[] parameterAnnotations) {
        return Optional.of(parameterAnnotations)
            .filter(a -> a.length > 0)
            .map(a -> ((Q) a[0]).value())
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
        return getClass().getSimpleName() + "[" + requestMethod + " " + path + (
            queryParameters.isEmpty() ? "" : "?" + String.join("&", queryParameters.values())
        ) + "]";
    }
}
