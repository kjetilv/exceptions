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

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

class ErrorHandler extends SimpleChannelInboundHandler<Object> {

    public static void notFound(ChannelHandlerContext ctx) {
        errorFuture(ctx, HttpResponseStatus.NOT_FOUND);
    }

    public static void internalError(ChannelHandlerContext ctx) {
        errorFuture(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        internalError(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        notFound(ctx);
    }

    private static void errorFuture(ChannelHandlerContext ctx, HttpResponseStatus notFound) {
        ctx.writeAndFlush(new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            notFound,
            Unpooled.buffer(0),
            new DefaultHttpHeaders(true).add(
                "Content-Length", 0),
            EmptyHttpHeaders.INSTANCE));
    }
}
