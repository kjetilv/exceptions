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

import java.util.Objects;
import java.util.function.Function;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import unearth.norest.common.Request;

public final class RequestWrapper extends SimpleChannelInboundHandler<FullHttpRequest> {
    
    private final Function<FullHttpRequest, Request> wrapper;
    
    public RequestWrapper(Function<FullHttpRequest, Request> wrapper) {
        this.wrapper = Objects.requireNonNull(wrapper, "wrapper");
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {
        ctx.fireChannelRead(wrapper.apply(msg));
    }
}
