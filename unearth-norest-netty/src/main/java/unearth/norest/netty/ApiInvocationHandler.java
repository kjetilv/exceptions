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
import java.util.function.Consumer;
import java.util.function.Function;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unearth.norest.IOHandler;
import unearth.norest.common.Request;
import unearth.norest.common.Response;

public class ApiInvocationHandler extends SimpleChannelInboundHandler<Request>
    implements Function<Request, Optional<Request>> {
    
    private static final Logger log = LoggerFactory.getLogger(ApiInvocationHandler.class);
    
    private final String prefix;
    
    private final IOHandler ioHandler;
    
    private final Function<Request, Optional<Object>> invoker;
    
    public ApiInvocationHandler(String prefix, IOHandler ioHandler, Function<Request, Optional<Object>> invoker) {
        this.prefix = prefix;
        this.ioHandler = ioHandler;
        this.invoker = invoker;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Request request) {
        try {
            process(request).ifPresentOrElse(
                ok(ctx),
                notFound(ctx, request));
        } catch (Exception e) {
            error(ctx, request, e);
        }
    }
    
    @Override
    public boolean isSharable() {
        return true;
    }
    
    @Override
    public Optional<Request> apply(Request request) {
        return prefixed(request);
    }
    
    private Optional<Request> prefixed(Request request) {
        return request.prefixed(prefix);
    }
    
    private Optional<Response> process(Request request) {
        return invoker.apply(request)
            .map(result ->
                new SimpleResponse(request, ioHandler.writeBytes(result)));
    }
    
    private static Consumer<Response> ok(ChannelHandlerContext ctx) {
        return ctx::writeAndFlush;
    }
    
    private static void error(ChannelHandlerContext ctx, Request request, Exception e) {
        log.error("Failed to serve {}", request, e);
        ctx.fireExceptionCaught(e);
    }
    
    private static Runnable notFound(ChannelHandlerContext ctx, Request request) {
        return () -> {
            log.warn("Not found: {}", request);
            ctx.fireChannelRead(request);
        };
    }
}
