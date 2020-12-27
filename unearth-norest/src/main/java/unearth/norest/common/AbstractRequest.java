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

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class AbstractRequest implements Request {

    private final Instant initTime;

    private final String uri;

    private final int queryIndex;

    private final String baseUri;

    protected AbstractRequest(String prefix, String uri, Instant initTime) {
        this.uri = normalized(prefix, uri);
        this.queryIndex = this.uri.indexOf('?');
        this.initTime = initTime;
        this.baseUri = getQueryIndex() < 0
            ? this.uri
            : this.uri.substring(0, getQueryIndex());
    }

    @Override
    public final Optional<Request> prefixed(String prefix) {
        return prefix == null ? Optional.of(this)
            : uri.startsWith(prefix) ? Optional.of(createPrefixed(prefix))
                : Optional.empty();
    }

    @Override
    public final RequestMethod getMethod() {
        return getMethod(getMethodName());
    }

    @Override
    public final String getPath() {
        return baseUri;
    }

    @Override
    public final int getQueryIndex() {
        return queryIndex;
    }

    @Override
    public final String getEntity() {
        return getBodyContent().toString();
    }

    @Override
    public final Map<String, String> getHeaders() {
        return retrieveHeaders().entrySet().stream()
            .filter(e ->
                !e.getValue().isEmpty())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e ->
                    single(e.getKey(), e.getValue())));
    }

    @Override
    public final Map<String, String> getQueryParameters() {
        int queryIndex = getQueryIndex();
        if (queryIndex < 0) {
            return Collections.emptyMap();
        }
        return Arrays.stream(uri.substring(queryIndex + 1).split("&"))
            .map(kv ->
                kv.split("=", 2))
            .collect(Collectors.toMap(
                kv -> kv[0],
                kv -> kv.length > 1 ? kv[1] : "",
                (s1, s2) -> {
                    throw new IllegalArgumentException("Not a 1-1 query parameters query: " + this);
                }));
    }

    @Override
    public final Duration timeTaken(Instant completionTime) {
        return Duration.between(initTime, completionTime);
    }

    protected abstract Request createPrefixed(String prefix);

    protected abstract CharSequence getBodyContent();

    protected abstract String getMethodName();

    protected abstract Map<String, List<String>> retrieveHeaders();

    protected final Instant getInitTime() {
        return initTime;
    }

    private static final Pattern LE_DEUX_SLASH = Pattern.compile("//");

    private static final Pattern BAD_TAIL = Pattern.compile("/\\?");

    private static RequestMethod getMethod(String methodName) {
        return switch (methodName.toUpperCase()) {
            case "GET" -> RequestMethod.GET;
            case "POST" -> RequestMethod.POST;
            case "PUT" -> RequestMethod.PUT;
            case "HEAD" -> RequestMethod.HEAD;
            case "PATCH" -> RequestMethod.PATCH;
            case "DELETE" -> RequestMethod.DELETE;
            default -> throw new IllegalArgumentException("Unsupported: " + methodName);
        };
    }

    private static String normalized(String prefix, String uri) {
        return "/" + goodTail(
            unpreslashed(
                unpostslashed(
                    unmidslashed(
                        urlPart(prefix, Objects.requireNonNull(uri, "uri"))))));
    }

    private static String goodTail(String suffixed) {
        return suffixed.contains("?/")
            ? suffixed
            : BAD_TAIL.matcher(suffixed).replaceAll("?");
    }

    private static String unpreslashed(String path) {
        return path.startsWith("/") ? unpreslashed(path.substring(1)) : path;
    }

    private static String unpostslashed(String path) {
        return path.endsWith("/") ? unpostslashed(path.substring(0, path.length() - 1)) : path;
    }

    private static String unmidslashed(String uri) {
        return uri.contains("//")
            ? unmidslashed(LE_DEUX_SLASH.matcher(uri).replaceAll("/"))
            : uri;
    }

    private static String urlPart(String prefix, String uri) {
        return prefix == null ? uri : uri.substring(prefix.length()).trim();
    }

    private static String single(String name, List<String> value) {
        if (value.size() > 1) {
            throw new IllegalStateException("Multi-value header: " + name + ": " + value);
        }
        return value.iterator().next();
    }
}
