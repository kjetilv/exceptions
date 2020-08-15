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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import unearth.norest.common.AbstractRequest;
import unearth.norest.common.Request;

public final class SimpleNettyRequest extends AbstractRequest {
    
    private final FullHttpRequest httpRequest;
    
    public SimpleNettyRequest(FullHttpRequest httpRequest, Instant time) {
        this(null, httpRequest, time);
    }
    
    public SimpleNettyRequest(String prefix, FullHttpRequest httpRequest, Instant time) {
        super(prefix,
            Objects.requireNonNull(httpRequest, "fullHttpRequest").uri(),
            time);
        this.httpRequest = httpRequest;
    }
    
    @Override
    protected Map<String, List<String>> retrieveHeaders() {
        HttpHeaders headers = httpRequest.headers();
        return headers.names().stream().collect(Collectors.toMap(
            Function.identity(),
            headers::getAllAsString));
    }
    
    @Override
    protected Request createPrefixed(String prefix) {
        return new SimpleNettyRequest(prefix, httpRequest, getInitTime());
    }
    
    @Override
    protected CharSequence getBodyContent() {
        ByteBuf content = this.httpRequest.content();
        return content.toString(StandardCharsets.UTF_8);
    }
    
    @Override
    protected String getMethodName() {
        return this.httpRequest.method().asciiName().toString();
    }
}
