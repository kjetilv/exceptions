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

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundInvoker;
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
import unearth.norest.ApiInvoker;
import unearth.norest.common.IOHandler;
import unearth.norest.common.Request;

public class ApiRouter<A> extends SimpleChannelInboundHandler<FullHttpRequest> {
    
    private static final Logger log = LoggerFactory.getLogger(ApiRouter.class);
    
    private final IOHandler ioHandler;
    
    private final ApiInvoker<A> apiInvoker;
    
    public ApiRouter(
        IOHandler ioHandler,
        ApiInvoker<A> apiInvoker
    ) {
        this.ioHandler = ioHandler;
        this.apiInvoker = apiInvoker;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) {
        Request request = new SimpleNettyRequest(fullHttpRequest);
        ChannelFuture future = respond(ctx, request);
        log.debug("Served {} -> {}", request, future);
    }
    
    private ChannelFuture respond(ChannelHandlerContext ctx, Request request) {
        try {
            return apiInvoker.response(request)
                .map(result ->
                    responseFuture(ctx, result))
                .orElseGet(() ->
                    ctx.writeAndFlush(error(HttpResponseStatus.NOT_FOUND)));
        } catch (Exception e) {
            log.error("Failed to serve {}", request, e);
            return ctx.writeAndFlush(error(HttpResponseStatus.INTERNAL_SERVER_ERROR));
        }
    }
    
    private ChannelFuture responseFuture(ChannelOutboundInvoker ctx, Object result) {
        byte[] bytes = ioHandler.writeBytes(result);
        HttpHeaders headers =
            new DefaultHttpHeaders(true).add("Content-Length", bytes.length);
        return ctx.writeAndFlush(
            new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(bytes),
                headers,
                EmptyHttpHeaders.INSTANCE));
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
