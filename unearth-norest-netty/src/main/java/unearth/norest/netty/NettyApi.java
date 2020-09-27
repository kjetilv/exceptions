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
import java.util.function.Function;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import unearth.norest.common.Request;
import unearth.norest.common.Response;
import unearth.norest.server.ApiInvoker;

public class NettyApi extends SimpleChannelInboundHandler<Request>
    implements Function<Request, Optional<Request>> {

    private final String prefix;

    private final ApiInvoker<?> invoker;

    public NettyApi(String prefix, ApiInvoker<?> invoker) {
        this.prefix = Objects.requireNonNull(prefix, "prefix");
        this.invoker = Objects.requireNonNull(invoker, "invoker");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Request request) {
        try {
            request.prefixed(prefix)
                .flatMap(req ->
                    invoker.response(req).map(result ->
                        new SimpleResponse(req, result)))
                .ifPresentOrElse(
                    msg ->
                        write(ctx, msg),
                    () ->
                        ctx.fireChannelRead(request));
        } catch (Exception e) {
            ctx.fireExceptionCaught(e);
        }
    }

    @Override
    public boolean isSharable() {
        return true;
    }

    @Override
    public Optional<Request> apply(Request request) {
        return request.prefixed(prefix);
    }

    private static void write(ChannelHandlerContext ctx, Response msg) {
        ctx.writeAndFlush(msg);
    }
}
