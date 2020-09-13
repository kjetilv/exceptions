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
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Function;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import unearth.norest.IOHandler;
import unearth.norest.common.Request;

public class NettyApi extends SimpleChannelInboundHandler<Request>
    implements Function<Request, Optional<Request>> {

    private final String prefix;

    private final IOHandler ioHandler;

    private final Executor executor;

    private final Function<Request, Optional<Object>> invoker;

    public NettyApi(
        String prefix,
        IOHandler ioHandler,
        Executor executor,
        Function<Request, Optional<Object>> invoker
    ) {
        this.prefix = Objects.requireNonNull(prefix, "prefix");
        this.ioHandler = Objects.requireNonNull(ioHandler, "ioHandler");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.invoker = Objects.requireNonNull(invoker, "invoker");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Request request) {
//        CompletableFuture.runAsync(
//            () -> {
                try {
                    request.prefixed(prefix)
                        .flatMap(req ->
                            invoker.apply(req).map(result ->
                                new SimpleResponse(req, ioHandler.writeBytes(result))))
                        .ifPresentOrElse(
                            ctx::writeAndFlush,
                            () ->
                                ctx.fireChannelRead(request));
                } catch (Exception e) {
                    ctx.fireExceptionCaught(e);
                }
//            },
//            executor);
    }

    @Override
    public boolean isSharable() {
        return true;
    }

    @Override
    public Optional<Request> apply(Request request) {
        return request.prefixed(prefix);
    }
}
