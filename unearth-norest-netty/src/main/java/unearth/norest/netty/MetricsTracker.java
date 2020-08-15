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

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import unearth.norest.common.Request;
import unearth.norest.common.Response;

public final class MetricsTracker {
    
    private final Supplier<Instant> time;
    
    private final Metrics metrics;
    
    private final Map<Request, Stats> stats = new ConcurrentHashMap<>();
    
    public MetricsTracker(Supplier<Instant> time, Metrics metrics) {
        this.time = time;
        this.metrics = metrics;
    }
    
    public SimpleChannelInboundHandler<Request> getInbound() {
        return new MetricsInbound();
    }
    
    public ChannelOutboundHandlerAdapter getOutbound() {
        return new MetricsOutbund();
    }
    
    private void updateStats(Request request) {
        stats.put(request, new Stats(request));
    }
    
    private void updateStats(Response msg) {
        stats.remove(msg.getRequest()).finish();
    }
    
    private class Stats {
        
        private final Request request;
        
        private final Instant startTime;
        
        Stats(Request request) {
            this.request = request;
            this.startTime = time.get();
            metrics.request(request.getMethod(), request.getPath(false))
                .increment();
        }
        
        void finish() {
            Instant endTime = time.get();
            metrics.requestTime(request.getMethod(), request.getPath(false))
                .record(Duration.between(startTime, endTime));
        }
    }
    
    private class MetricsInbound extends SimpleChannelInboundHandler<Request> {
        
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Request request) {
            try {
                updateStats(request);
            } finally {
                ctx.fireChannelRead(request);
            }
        }
    }
    
    private class MetricsOutbund extends ChannelOutboundHandlerAdapter {
        
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            try {
                if (msg instanceof Response) {
                    updateStats((Response) msg);
                }
            } finally {
                ctx.write(msg, promise);
            }
        }
    }
}
