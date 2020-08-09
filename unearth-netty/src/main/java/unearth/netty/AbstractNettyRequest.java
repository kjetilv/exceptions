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

package unearth.netty;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import unearth.norest.common.Request;
import unearth.util.Streams;

abstract class AbstractNettyRequest implements Request {
    
    private final FullHttpRequest fullHttpRequest;
    
    private final String uri;
    
    private final int queryIndex;
    
    AbstractNettyRequest(FullHttpRequest fullHttpRequest) {
        this.fullHttpRequest = Objects.requireNonNull(fullHttpRequest, "fullHttpRequest");
        this.uri = fullHttpRequest.uri();
        this.queryIndex = this.uri.indexOf('?');
    }
    
    @Override
    public int getQueryIndex() {
        return queryIndex;
    }
    
    @Override
    public List<String> getPathParameters(Matcher matcher) {
        return Streams.matches(matcher).collect(Collectors.toList());
    }
    
    protected String lazyGetEntity() {
        ByteBuf content = fullHttpRequest.content();
        CharSequence body = content.toString(StandardCharsets.UTF_8);
        return body.toString();
    }
    
    protected Map<String, String> lazyGetSingleHeaders() {
        return collapse(getHeaders());
    }
    
    protected Map<String, String> lazyGetSingleQueryParameters() {
        return collapse(getQueryParameters());
    }
    
    protected Map<String, Collection<String>> lazyGetQueryParameters() {
        int queryIndex = getQueryIndex();
        if (queryIndex < 0) {
            return Collections.emptyMap();
        }
        return Arrays.stream(uri.substring(queryIndex + 1).split("&"))
            .map(kv ->
                kv.split("=", 2))
            .collect(Collectors.groupingBy(
                keyValue -> keyValue[0],
                Collectors.mapping(
                    kv1 -> kv1.length > 1 ? kv1[1] : "",
                    Collectors.toCollection(HashSet::new))));
    }
    
    protected Map<String, Collection<String>> lazyGetHeaders() {
        HttpHeaders headers = fullHttpRequest.headers();
        return headers.names().stream()
            .collect(Collectors.toMap(
                Function.identity(),
                headers::getAll));
    }
    
    protected Request.Method lazyGetMethod() {
        return switch (fullHttpRequest.method().asciiName().toString()) {
            case "GET" -> Request.Method.GET;
            case "POST" -> Request.Method.POST;
            case "PUT" -> Request.Method.PUT;
            case "HEAD" -> Request.Method.HEAD;
            case "PATCH" -> Request.Method.PATCH;
            case "DELETE" -> Request.Method.DELETE;
            default -> throw new IllegalArgumentException("Unsupported: " + fullHttpRequest.method());
        };
    }
    
    protected String getUri() {
        return uri;
    }
    
    private static Map<String, String> collapse(Map<String, Collection<String>> values) {
        return values.entrySet().stream()
            .filter(e -> e.getValue() != null)
            .filter(e -> e.getValue().size() == 1)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().iterator().next()
            ));
    }
}
