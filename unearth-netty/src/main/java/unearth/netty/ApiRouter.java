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

import java.util.Objects;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unearth.norest.common.IOHandler;
import unearth.norest.server.InvokableMethods;

public class ApiRouter<A> extends SimpleChannelInboundHandler<FullHttpRequest> {
    
    private static final Logger log = LoggerFactory.getLogger(ApiRouter.class);
    
    private final A impl;
    
    private final InvokableMethods<A> invokableMethods;
    
    private final IOHandler ioHandler;
    
    public ApiRouter(A impl, InvokableMethods<A> invokableMethods, IOHandler ioHandler) {
        this.impl = Objects.requireNonNull(impl, "impl");
        this.invokableMethods = invokableMethods;
        this.ioHandler = ioHandler;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) {
        NettyRequest request = new NettyRequest(fullHttpRequest);
        ChannelFuture channelFuture = invokableMethods.invoke(request)
            .map(invoker ->
                invoker.on(impl))
            .map(result ->
                responseFuture(ctx, result))
            .findFirst()
            .orElseGet(() ->
                ctx.writeAndFlush(NOT_FOUND));
        log.debug("{} -> {}", fullHttpRequest, channelFuture);
    }
    
    private ChannelFuture responseFuture(ChannelHandlerContext ctx, Object result) {
        byte[] array = ioHandler.writeBytes(result);
        ByteBuf buffer = Unpooled.wrappedBuffer(array);
        HttpHeaders headers = new DefaultHttpHeaders(true).add(
            "Content-Length", array.length);
        return ctx.writeAndFlush(
            new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                buffer,
                headers,
                EmptyHttpHeaders.INSTANCE));
    }
    
    private static final DefaultFullHttpResponse NOT_FOUND = new DefaultFullHttpResponse(
        HttpVersion.HTTP_1_1,
        HttpResponseStatus.NOT_FOUND,
        Unpooled.buffer(0),
        new DefaultHttpHeaders(true).add(
            "Content-Length", 0),
        EmptyHttpHeaders.INSTANCE
    );
}
