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

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public class HealthServer extends SimpleChannelInboundHandler<FullHttpRequest> {
    
    private final String path;
    
    private final Supplier<Health> healthSupplier;
    
    public HealthServer(String path, Supplier<Health> healthSupplier) {
        this.path = path;
        this.healthSupplier = healthSupplier;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {
        if (msg.uri().startsWith(path)) {
            Health health = healthSupplier.get();
            ctx.writeAndFlush(new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.valueOf(health.getStatus()),
                Unpooled.buffer(health.name().length())
                    .writeBytes(health.name().getBytes(StandardCharsets.UTF_8)),
                new DefaultHttpHeaders(true)
                    .add("Content-Type", "text/plain"),
                EmptyHttpHeaders.INSTANCE));
        } else {
            ctx.fireChannelRead(msg);
        }
    }
}
