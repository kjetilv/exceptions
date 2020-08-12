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

import java.util.Optional;
import java.util.function.Function;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundInvoker;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unearth.norest.common.IOHandler;
import unearth.norest.common.Request;

@ChannelHandler.Sharable
public class ApiRouter extends SimpleChannelInboundHandler<Request> {
    
    private static final Logger log = LoggerFactory.getLogger(ApiRouter.class);
    
    private final String prefix;
    
    private final IOHandler ioHandler;
    
    private final Function<Request, Optional<Object>> invoker;
    
    public ApiRouter(String prefix, IOHandler ioHandler, Function<Request, Optional<Object>> invoker) {
        this.prefix = prefix;
        this.ioHandler = ioHandler;
        this.invoker = invoker;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Request request) {
        try {
            Optional<Object> response = request.prefixed(prefix)
                .flatMap(req ->
                    invoker.apply(req).map(res ->
                        respond(ctx, res)));
            if (response.isEmpty()) {
                ctx.fireChannelRead(request);
            }
        } catch (Exception e) {
            log.error("Failed to serve {} {}", request.getMethod(), request.getPath(false), e);
            ctx.fireExceptionCaught(e);
        }
    }
    
    private Object respond(ChannelOutboundInvoker ctx, Object result) {
        byte[] bytes = ioHandler.writeBytes(result);
        ByteBuf content = Unpooled.wrappedBuffer(bytes);
        HttpHeaders headers = new DefaultHttpHeaders(true)
            .add("Content-Length", bytes.length);
        ctx.writeAndFlush(
            new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                content,
                headers,
                EmptyHttpHeaders.INSTANCE));
        return result;
    }
}
