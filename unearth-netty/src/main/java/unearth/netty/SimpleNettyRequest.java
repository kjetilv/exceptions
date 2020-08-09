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

public class SimpleNettyRequest implements Request {
    
    private final FullHttpRequest fullHttpRequest;
    
    private final String uri;
    
    private final int queryIndex;
    
    SimpleNettyRequest(FullHttpRequest fullHttpRequest) {
        this.fullHttpRequest = Objects.requireNonNull(fullHttpRequest, "fullHttpRequest");
        this.uri = fullHttpRequest.uri();
        this.queryIndex = uri.indexOf('?');
    }
    
    @Override
    public Method getMethod() {
        return switch (this.fullHttpRequest.method().asciiName().toString()) {
            case "GET" -> Method.GET;
            case "POST" -> Method.POST;
            case "PUT" -> Method.PUT;
            case "HEAD" -> Method.HEAD;
            case "PATCH" -> Method.PATCH;
            case "DELETE" -> Method.DELETE;
            default -> throw new IllegalArgumentException("Unsupported: " +
                SimpleNettyRequest.this.fullHttpRequest.method());
        };
    }
    
    @Override
    public String getPath() {
        return uri;
    }
    
    @Override
    public int getQueryIndex() {
        return queryIndex;
    }
    
    @Override
    public Map<String, Collection<String>> getQueryParameters() {
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
    
    @Override
    public Map<String, Collection<String>> getHeaders() {
        HttpHeaders headers = fullHttpRequest.headers();
        return headers.names().stream()
            .collect(Collectors.toMap(
                Function.identity(),
                headers::getAll));
    }
    
    @Override
    public List<String> getPathParameters(Matcher matcher) {
        return Streams.matches(matcher).collect(Collectors.toList());
    }
    
    @Override
    public String getEntity() {
        ByteBuf content = this.fullHttpRequest.content();
        CharSequence body = content.toString(StandardCharsets.UTF_8);
        return body.toString();
    }
    
    @Override
    public Map<String, String> getSingleQueryParameters() {
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
    public Map<String, String> getSingleHeaders() {
        HttpHeaders headers = fullHttpRequest.headers();
        return headers.names().stream()
            .collect(Collectors.toMap(
                Function.identity(),
                headers::get));
    }
}
