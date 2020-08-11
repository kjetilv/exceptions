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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;
import static java.lang.StrictMath.toIntExact;

@SuppressWarnings("SameParameterValue")
public final class NettyServer {
    
    private static final Logger log = LoggerFactory.getLogger(NettyServer.class);
    
    private final int listenThreads;
    
    private final int workThreads;
    
    private final int threads;
    
    private final int queue;
    
    private final Duration connectTimeout;
    
    private final int port;
    
    private final EventLoopGroup listen;
    
    private final EventLoopGroup work;
    
    private final AtomicReference<ChannelFuture> started = new AtomicReference<>();
    
    private final AtomicBoolean closed = new AtomicBoolean();
    
    private final List<ChannelHandler> handlers;
    
    public NettyServer(int port, ChannelHandler... handlers) {
        if (handlers.length == 0) {
            throw new IllegalArgumentException("No handlers");
        }
        this.port = port;
        this.handlers = Arrays.asList(handlers);
        
        this.listenThreads = Runtime.getRuntime().availableProcessors();
        this.workThreads = 10;
        this.threads = 32;
        this.queue = 32;
        this.connectTimeout = Duration.ofSeconds(10);
        
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            threads,
            threads,
            60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(queue),
            countingThreadFactory("work"),
            new ThreadPoolExecutor.CallerRunsPolicy());
        this.listen = new NioEventLoopGroup(
            this.listenThreads,
            executor);
        this.work = new NioEventLoopGroup(
            this.workThreads,
            executor);
    }
    
    public void start() {
        started.updateAndGet(exsting -> exsting != null ? exsting
            : new ServerBootstrap()
                .group(listen, work)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInit(handlers))
                .option(
                    CONNECT_TIMEOUT_MILLIS,
                    toIntExact(connectTimeout.toMillis()))
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .bind(port)
                .addListener(future ->
                    log.info("{}: Bound to port {}", this, port)));
    }
    
    public void stop() {
        if (closed.compareAndSet(false, true)) {
            shutdown();
        }
    }
    
    private void shutdown() {
        try {
            started.get().channel().close().addListener(future ->
                log.info("{}: closed {}: {}", this, port, future));
        } finally {
            listen.shutdownGracefully();
            work.shutdownGracefully();
        }
    }
    
    private static final int MAX_CONTENT_LENGTH = 16 * 1024 * 1026;
    
    private static ThreadFactory countingThreadFactory(String prefix) {
        LongAdder count = new LongAdder();
        return runnable -> {
            try {
                return new Thread(runnable, prefix + '#' + count.longValue());
            } finally {
                count.increment();
            }
        };
    }
    
    private static final class ChannelInit extends ChannelInitializer<NioSocketChannel> {
        
        private final List<ChannelHandler> handlers;
        
        private ChannelInit(List<ChannelHandler> handlers) {
            this.handlers = handlers;
        }
        
        @Override
        protected void initChannel(NioSocketChannel channel) {
            ChannelPipeline pipeline = channel.pipeline()
                .addLast("decoder", new HttpServerCodec())
                .addLast("aggregator", new HttpObjectAggregator(MAX_CONTENT_LENGTH));
            handlers.forEach(handler1 ->
                pipeline.addLast(handler1.getClass().getSimpleName(), handler1));
        }
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" +
            "l:" + listenThreads + "/w:" + workThreads + " -> t:" + threads + "/q:" + queue +
            "]";
    }
}
