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
import java.util.Optional;
import java.util.function.Supplier;

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
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unearth.norest.common.IOHandler;
import unearth.norest.common.Request;
import unearth.norest.server.ForwardableMethods;

public class ApiRouter<A> extends SimpleChannelInboundHandler<FullHttpRequest> {
    
    private static final Logger log = LoggerFactory.getLogger(ApiRouter.class);
    
    private final A impl;
    
    private final ForwardableMethods<A> forwardableMethods;
    
    private final IOHandler ioHandler;
    
    public ApiRouter(A impl, ForwardableMethods<A> forwardableMethods, IOHandler ioHandler) {
        this.impl = Objects.requireNonNull(impl, "impl");
        this.forwardableMethods = forwardableMethods;
        this.ioHandler = ioHandler;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) {
        Request request = request(fullHttpRequest);
        ChannelFuture future = serve(ctx, request);
        log.debug("Served {} -> {}", request, future);
    }
    
    private ChannelFuture serve(ChannelHandlerContext ctx, Request request) {
        try {
            Optional<Object> response = response(request);
            return response
                .map(result ->
                    responseFuture(ctx, result))
                .orElseGet(
                    error(ctx, HttpResponseStatus.NOT_FOUND));
        } catch (Exception e) {
            log.error("Failed to serve {}", request, e);
            return error(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR).get();
        }
    }
    
    private Optional<Object> response(Request request) {
        return forwardableMethods.invocation(request)
            .map(invoke ->
                invoke.apply(impl))
            .findFirst();
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
    
    private static Request request(FullHttpRequest fullHttpRequest) {
        return new SimpleNettyRequest(fullHttpRequest);
    }
    
    private static Supplier<ChannelFuture> error(ChannelHandlerContext ctx, HttpResponseStatus status) {
        return () -> respond(ctx, status);
    }
    
    private static ChannelFuture respond(ChannelHandlerContext ctx, HttpResponseStatus status) {
        return ctx.writeAndFlush(error(status));
    }
    
    private static HttpResponse error(HttpResponseStatus status) {
        return new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            status,
            Unpooled.buffer(0),
            new DefaultHttpHeaders(true).add(
                "Content-Length", 0),
            EmptyHttpHeaders.INSTANCE
        );
    }
}
