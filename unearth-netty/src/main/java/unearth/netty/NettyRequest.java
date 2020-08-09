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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import unearth.norest.common.Request;
import unearth.util.Streams;
import unearth.util.once.Get;

class NettyRequest implements Request {
    
    private final FullHttpRequest fullHttpRequest;
    
    private final Supplier<Method> method;
    
    private final Supplier<Map<String, Collection<String>>> headers;
    
    private final Supplier<Map<String, Collection<String>>> queryParameters;
    
    NettyRequest(FullHttpRequest fullHttpRequest) {
        this.fullHttpRequest = fullHttpRequest;
        
        this.headers = Get.mostlyOnce(this::lazyGetHeaders);
        this.method = Get.mostlyOnce(this::lazyGetMethod);
        this.queryParameters = Get.mostlyOnce(this::lazyGetQueryParameters);
    }
    
    @Override
    public Method getMethod() {
        return this.method.get();
    }
    
    @Override
    public String getPath() {
        return fullHttpRequest.uri();
    }
    
    @Override
    public Map<String, Collection<String>> getQueryParameters() {
        return this.queryParameters.get();
    }
    
    @Override
    public Map<String, Collection<String>> getHeaders() {
        return this.headers.get();
    }
    
    @Override
    public String getEntity() {
        ByteBuf content = fullHttpRequest.content();
        CharSequence body = content.toString(StandardCharsets.UTF_8);
        return body.toString();
    }
    
    @Override
    public List<String> getPathParameters(Matcher matcher) {
        return Streams.matches(matcher).collect(Collectors.toList());
    }
    
    private Map<String, Collection<String>> lazyGetQueryParameters() {
        String[] split = fullHttpRequest.uri().split("\\?", 2);
        if (split.length < 2) {
            return Collections.emptyMap();
        }
        return Arrays.stream(split[1].split(":"))
            .map(keyvalue ->
                keyvalue.split("=", 2))
            .collect(Collectors.groupingBy(
                NettyRequest::key,
                Collectors.mapping(
                    NettyRequest::value,
                    Collectors.toCollection(HashSet::new))));
    }
    
    private Map<String, Collection<String>> lazyGetHeaders() {
        HttpHeaders headers = fullHttpRequest.headers();
        return headers.names().stream()
            .collect(Collectors.toMap(
                Function.identity(),
                headers::getAll));
    }
    
    private Method lazyGetMethod() {
        return switch (fullHttpRequest.method().asciiName().toString()) {
            case "GET" -> Method.GET;
            case "POST" -> Method.POST;
            case "PUT" -> Method.PUT;
            case "HEAD" -> Method.HEAD;
            case "PATCH" -> Method.PATCH;
            case "DELETE" -> Method.DELETE;
            default -> throw new IllegalArgumentException("Unsupported: " + fullHttpRequest.method());
        };
    }
    
    private static String key(String[] keyValue) {
        return keyValue[0];
    }
    
    private static String value(String[] keyValue) {
        return keyValue.length > 1 ? keyValue[1] : "";
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getMethod() + " " + fullHttpRequest.uri() + "]";
    }
}
