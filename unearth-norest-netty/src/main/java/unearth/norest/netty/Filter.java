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

import java.util.function.BiFunction;
import java.util.function.Predicate;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

@ChannelHandler.Sharable
public class Filter extends SimpleChannelInboundHandler<HttpRequest> {

    private final Predicate<HttpRequest> test;

    private final BiFunction<ChannelHandlerContext, HttpRequest, HttpResponse> fail;

    public Filter(Predicate<HttpRequest> test, BiFunction<ChannelHandlerContext, HttpRequest, HttpResponse> fail) {
        this.test = test;
        this.fail = fail;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest msg) {
        if (test.test(msg)) {
            ctx.fireChannelRead(msg);
        } else {
            ctx.writeAndFlush(fail.apply(ctx, msg));
        }
    }
}
