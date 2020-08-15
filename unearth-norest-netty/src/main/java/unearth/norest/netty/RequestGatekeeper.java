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
import io.netty.handler.codec.http.FullHttpRequest;
import unearth.norest.common.Request;

public final class RequestGatekeeper extends SimpleChannelInboundHandler<FullHttpRequest> {
    
    private final RequestFactory wrapper;
    
    private final Function<Request, Optional<Request>> prefixer;
    
    public RequestGatekeeper(
        Function<Request, Optional<Request>> prefixer,
        RequestFactory wrapper
    ) {
        this.wrapper = Objects.requireNonNull(wrapper, "wrapper");
        this.prefixer = prefixer;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {
        prefixer.apply(wrapper.create(msg)).ifPresentOrElse(
            ctx::fireChannelRead,
            () ->
                ErrorHandler.notFound(ctx));
    }
}
