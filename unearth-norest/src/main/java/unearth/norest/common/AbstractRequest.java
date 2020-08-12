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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class AbstractRequest implements Request {
    
    private final String uri;
    
    private final int queryIndex;
    
    protected AbstractRequest(String prefix, String uri) {
        this.uri = normalized(prefix, uri);
        this.queryIndex = this.uri.indexOf('?');
    }
    
    @Override
    public Optional<Request> prefixed(String prefix) {
        if (prefix == null) {
            return Optional.of(this);
        }
        if (uri.startsWith(prefix)) {
            return Optional.of(createPrefixed(prefix));
        }
        return Optional.empty();
    }
    
    @Override
    public RequestMethod getMethod() {
        return getMethod(getMethodName());
    }
    
    @Override
    public String getPath(boolean withQueryParameters) {
        return withQueryParameters || !hasQueryParameters() ? uri
            : uri.substring(0, getQueryIndex());
    }
    
    @Override
    public int getQueryIndex() {
        return queryIndex;
    }
    
    @Override
    public String getEntity() {
        return getBodyContent().toString();
    }
    
    @Override
    public Map<String, String> getHeaders() {
        return retrieveHeaders().entrySet().stream()
            .filter(e ->
                !e.getValue().isEmpty())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> single(e.getKey(), e.getValue())));
    }
    
    @Override
    public Map<String, String> getQueryParameters() {
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
    
    protected abstract Request createPrefixed(String prefix);
    
    protected abstract CharSequence getBodyContent();
    
    protected abstract String getMethodName();
    
    protected abstract Map<String, List<String>> retrieveHeaders();
    
    private static final String BAD_TAIL = "/?";
    
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
    
    private static String unpreslashed(String path) {
        return path.startsWith("/") ? unpreslashed(path.substring(1)) : path;
    }
    
    private static String unpostslashed(String path) {
        return path.endsWith("/") ? unpostslashed(path.substring(0, path.length() - 1)) : path;
    }
    
    protected static String single(String name, List<String> value) {
        if (value.size() > 1) {
            throw new IllegalStateException("Multi-value header: " + name + ": " + value);
        }
        return value.iterator().next();
    }
    
    protected static String normalized(String prefix, String uri) {
        String suffixed = prefix == null ? uri : uri.substring(prefix.length()).trim();
        int badTail = suffixed.indexOf(BAD_TAIL);
        String fixed = badTail < 0
            ? suffixed
            : suffixed.substring(0, badTail) + '?' + suffixed.substring(badTail + 2);
        return "/" + unpreslashed(unpostslashed(fixed));
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getMethodName() + " " + uri + "]";
    }
}
