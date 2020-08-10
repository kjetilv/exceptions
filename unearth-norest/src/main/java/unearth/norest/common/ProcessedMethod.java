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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import unearth.norest.DELETE;
import unearth.norest.GET;
import unearth.norest.HEAD;
import unearth.norest.POST;
import unearth.norest.PUT;
import unearth.norest.Q;
import unearth.norest.client.RemotableMethod;
import unearth.norest.server.ForwardableMethod;

public final class ProcessedMethod implements RemotableMethod, ForwardableMethod {
    
    private final RequestMethod httpMethod;
    
    private final String path;
    
    private final String rootPath;
    
    private final Map<Integer, String> queryParameters;
    
    private final Map<Integer, String> pathParameters;
    
    private final boolean stringBody;
    
    private final Class<?> returnType;
    
    private final boolean optionalReturn;
    
    private final boolean returnsData;
    
    private final Pattern matchPattern;
    
    private final Transformers transformers;
    
    private final Method method;
    
    private final Class<?>[] parameterTypes;
    
    private final Parameter[] parameters;
    
    private final Annotation[][] parameterAnnotations;
    
    private final String[] parameterNames;
    
    private final int bodyArgumentIndex;
    
    public ProcessedMethod(Method method, Transformers transformers) {
        this.method = Objects.requireNonNull(method, "method");
        
        Annotation annotation = getAnnotation(this.method);
        String annotatedPath = path(annotation);
        this.path = normalized(annotatedPath);
        
        int rootIndex = rootIndex(this.path);
        this.rootPath = rootIndex < 0 ? this.path : this.path.substring(0, rootIndex);
        this.httpMethod = httpMethod(annotation);
        
        String regex = PATH_ARG.matcher(this.path).replaceAll("\\([^/]*\\)");
        this.matchPattern = Pattern.compile(regex);
        
        Class<?> returnType = this.method.getReturnType();
        this.parameterTypes = this.method.getParameterTypes();
        this.parameters = this.method.getParameters();
        this.parameterAnnotations = this.method.getParameterAnnotations();
        this.parameterNames =
            IntStream.range(0, parameters.length).mapToObj(this::paramName).toArray(String[]::new);
        
        this.optionalReturn = Optional.class.isAssignableFrom(returnType);
        this.returnsData = returnType != void.class;
        this.returnType = getActualReturnType(this.method, this.optionalReturn, returnType);
        
        this.stringBody = this.httpMethod.isEntity() && parameterTypes[0] == String.class;
        this.bodyArgumentIndex = this.httpMethod.isEntity() ?
            IntStream.range(0, parameterAnnotations.length)
                .filter(i -> parameterAnnotations[i] == null || parameterAnnotations[i].length == 0)
                .findFirst()
                .orElseThrow(() ->
                    new IllegalStateException("No body argument could be derived for " + method)) :
            -1;
        
        this.queryParameters = paramsWhere(i ->
            parameterAnnotations[i].length > 0);
        this.pathParameters = httpMethod.isEntity()
            ? Collections.emptyMap()
            : paramsWhere(i ->
                parameterAnnotations[i] == null || parameterAnnotations[i].length == 0);
        this.transformers = transformers == null ? Transformers.EMPTY : transformers;
    }
    
    @Override
    public Stream<Function<Object, Object>> matchingInvoker(Request request) {
        if (request.getMethod() != this.httpMethod) {
            return Stream.empty();
        }
        String requestedPath = request.getPath();
        String path = normalized(requestedPath);
        if (!path.startsWith(rootPath)) {
            return Stream.empty();
        }
        if (path.equals(rootPath)) {
            return Stream.of(invoker(request, null));
        }
        int queryIndex = request.getQueryIndex();
        if (queryIndex < 0) {
            return matching(request, requestedPath);
        }
        return matching(request, requestedPath.substring(0, queryIndex));
    }
    
    @Override
    public RequestMethod getRequestMethod() {
        return httpMethod;
    }
    
    @Override
    public String getContentType() {
        return stringBody ? TEXT : JSON;
    }
    
    @Override
    public boolean isStringBody() {
        return stringBody;
    }
    
    @Override
    public Optional<Object> bodyArgument(Object... args) {
        return httpMethod.isEntity()
            ? Optional.ofNullable(args[bodyArgumentIndex])
            : Optional.empty();
    }
    
    @Override
    public String path(Object... args) {
        if (args == null || args.length == 0 || httpMethod.isEntity()) {
            return path;
        }
        Object arg = args[0];
        String fullPath = toString(arg)
            .map(string -> path.replace(PAR, string))
            .orElseThrow(() ->
                new IllegalArgumentException("Not a recognized  path parameter: " + arg));
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
    
    private Stream<Function<Object, Object>> matching(Request request, String requestedPath) {
        Matcher matcher = matchPattern.matcher(requestedPath);
        if (matcher.matches()) {
            return Stream.of(invoker(request, matcher));
        }
        return Stream.empty();
    }
    
    private Function<Object, Object> invoker(Request request, Matcher matcher) {
        return impl ->
            invoke(request, matcher, impl);
    }
    
    private Map<Integer, String> paramsWhere(IntPredicate intPredicate) {
        return IntStream.range(0, parameterAnnotations.length)
            .filter(intPredicate)
            .boxed()
            .collect(Collectors.toMap(i -> i, i -> parameterNames[i]));
    }
    
    private Object invoke(Request request, Matcher matcher, Object impl) {
        Map<String, String> queryParams = request.getQueryParameters();
        Map<String, Optional<?>> queryArgs = IntStream.range(0, parameters.length)
            .boxed()
            .collect(Collectors.toMap(
                i -> parameterNames[i],
                i ->
                    transformers.from(parameterTypes[i], queryParams.get(parameterNames[i]))));
        List<String> pathParams = matches(matcher);
        if (pathParams.size() != pathParameters.size()) {
            throw new IllegalArgumentException(this + " got bad path: " + request);
        }
        Map<String, Optional<?>> pathArgs = pathParameters.entrySet().stream()
            .collect(Collectors.toMap(
                e -> parameterNames[e.getKey()],
                e ->
                    transformers.from(parameterTypes[e.getKey()], pathParams.get(e.getKey()))));
        Object[] args = Arrays.stream(parameterNames)
            .map(name -> lookup(name, queryArgs, pathArgs))
            .map(opt -> opt.orElse(null))
            .toArray(Object[]::new);
        if (httpMethod.isEntity()) {
            String entity = request.getEntity();
            args[bodyArgumentIndex] = stringBody
                ? entity
                : transformers.from(parameterTypes[bodyArgumentIndex], entity);
        }
        Object result;
        try {
            result = method.invoke(impl, args);
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to invoke on " + impl + ": " + method + "" + Arrays.toString(args), e);
        }
        if (result == null || !isReturnData()) {
            return null;
        }
        if (isReturnOptional()) {
            return ((Optional<?>) result).orElse(null);
        }
        return result;
    }
    
    private String paramName(int index) {
        if (index == bodyArgumentIndex) {
            return "";
        }
        return annotatedName(parameterAnnotations[index])
            .or(() ->
                parameters[index].isNamePresent()
                    ? Optional.of(parameters[index].getName())
                    : Optional.empty())
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Could not extract argument name #" + index + ": " + parameters[index]));
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
    
    @SuppressWarnings("unchecked")
    private <T> Optional<String> toString(T arg) {
        return this.transformers.to((Class<T>) arg.getClass(), arg);
    }
    
    private static final String JSON = "application/json;charset=UTF-8";
    
    private static final String TEXT = "text/plain;charset=UTF-8";
    
    private static final String PAR = "{}";
    
    private static final Pattern PATH_ARG = Pattern.compile("\\{\s*}");
    
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
    
    private static List<String> matches(Matcher matcher) {
        if (matcher == null) {
            return Collections.emptyList();
        }
        
        int groupCount = matcher.groupCount();
        if (groupCount == 0) {
            return Collections.emptyList();
        }
        
        return StreamSupport.stream(new MatchSpliterator(matcher), false)
            .collect(Collectors.toList());
    }
    
    @SafeVarargs
    private static Optional<?> lookup(String name, Map<String, Optional<?>>... maps) {
        return Arrays.stream(maps)
            .map(map -> map.getOrDefault(name, Optional.empty()))
            .flatMap(Optional::stream)
            .findFirst();
    }
    
    private static RequestMethod httpMethod(Annotation annotation) {
        return annotation instanceof POST ? RequestMethod.POST
            : annotation instanceof PUT ? RequestMethod.PUT
                : RequestMethod.GET;
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
    
    private static class MatchSpliterator extends Spliterators.AbstractSpliterator<String> {
        
        private int group;
        
        private final int groupCount;
        
        private final Matcher matcher;
        
        private MatchSpliterator(Matcher matcher) {
            super(matcher.groupCount(), Spliterator.ORDERED);
            this.groupCount = matcher.groupCount();
            this.matcher = matcher;
            group = 0;
        }
        
        @Override
        public boolean tryAdvance(Consumer<? super String> action) {
            action.accept(matcher.group(group + 1));
            group++;
            return group < groupCount;
        }
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + httpMethod + " " + path + (
            queryParameters.isEmpty() ? "" : "?" + String.join("&", queryParameters.values())
        ) + "]";
    }
}
