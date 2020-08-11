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

package unearth.norest.netty;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import unearth.norest.common.Request;
import unearth.norest.common.RequestMethod;

public class SimpleNettyRequest implements Request {
    
    private final FullHttpRequest httpRequest;
    
    private final String uri;
    
    private final int queryIndex;
    
    public SimpleNettyRequest(FullHttpRequest httpRequest) {
        this(null, null, httpRequest);
    }
    
    private SimpleNettyRequest(String prefix, String uri, FullHttpRequest httpRequest) {
        this.httpRequest = Objects.requireNonNull(httpRequest, "fullHttpRequest");
        this.uri = uri(prefix, uri == null ? httpRequest.uri() : uri);
        this.queryIndex = this.uri.indexOf('?');
    }
    
    @Override
    public Request suffix(String prefix) {
        return prefix == null
            ? this
            : new SimpleNettyRequest(prefix, uri, httpRequest);
    }
    
    @Override
    public RequestMethod getMethod() {
        return switch (this.httpRequest.method().asciiName().toString()) {
            case "GET" -> RequestMethod.GET;
            case "POST" -> RequestMethod.POST;
            case "PUT" -> RequestMethod.PUT;
            case "HEAD" -> RequestMethod.HEAD;
            case "PATCH" -> RequestMethod.PATCH;
            case "DELETE" -> RequestMethod.DELETE;
            default -> throw new IllegalArgumentException("Unsupported: " +
                SimpleNettyRequest.this.httpRequest.method());
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
    public String getEntity() {
        ByteBuf content = this.httpRequest.content();
        CharSequence body = content.toString(StandardCharsets.UTF_8);
        return body.toString();
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
    
    private static String uri(String prefix, String uri) {
        if (prefix == null) {
            return clean(uri);
        }
        if (!uri.startsWith(prefix)) {
            throw new IllegalArgumentException("Invalid prefix for " + uri + ": " + prefix);
        }
        return clean(uri.substring(prefix.length()));
    }
    
    private static String clean(String uri) {
        int badTail = uri.indexOf("/?");
        if (badTail < 0) {
            return uri.trim();
        }
        return uri.substring(0, badTail) + '?' + uri.substring(badTail + 2).trim();
    }
}
