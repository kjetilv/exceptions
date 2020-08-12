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
import java.util.Objects;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import unearth.norest.common.AbstractRequest;

public final class SimpleNettyRequest extends AbstractRequest {
    
    private final FullHttpRequest httpRequest;
    
    public SimpleNettyRequest(String prefix, FullHttpRequest httpRequest) {
        super(prefix, Objects.requireNonNull(httpRequest, "fullHttpRequest").uri());
        this.httpRequest = httpRequest;
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
