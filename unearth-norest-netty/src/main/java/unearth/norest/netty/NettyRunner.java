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

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unearth.metrics.MetricsFactory;
import unearth.norest.traffic.RateLimiter;
import unearth.util.once.Get;

import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;

public final class NettyRunner {

    private static final Logger log = LoggerFactory.getLogger(NettyRunner.class);

    private final Supplier<EventLoopGroup> listen;

    private final Supplier<EventLoopGroup> work;

    private final Supplier<ChannelFuture> started;

    public NettyRunner(
        int port,
        NettyApi api,
        MetricsFactory metricsFactory,
        Supplier<byte[]> metricsOut,
        Clock clock
    ) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            32,
            64,
            60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(64),
            countingThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy());

        this.listen = Get.once(() ->
            new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(), executor));
        this.work = Get.once(() ->
            new NioEventLoopGroup(10, executor));
        this.started = Get.once(() -> {
            RequestFactory requestFactory = httpRequest ->
                new SimpleNettyRequest(httpRequest, clock.instant());
            MetricsTracker metricsTracker =
                new MetricsTracker(clock::instant, () -> metricsFactory.instantiate(Metrics.class));
            Filter filter = new Filter(
                new RateLimiter<>(
                    clock::instant,
                    1000,
                    1000,
                    Duration.ofMinutes(1),
                    60),
                (ctx, req) ->
                    new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.TOO_MANY_REQUESTS));

            ChannelInitializer<Channel> childHandler = new ChannelInitializer<>() {

                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(
                        new HttpServerCodec(),
                        new HttpObjectAggregator(MAX_CONTENT_LENGTH),
                        new Slasher(),
                        new HealthServer(HEALTH_PATH, () -> Health.OK),
                        new MetricsServer(METRICS_PATH, metricsOut),
                        new RequestReader(requestFactory),
                        new ResponseWriter(),
                        filter,
                        metricsTracker.getOutbound(),
                        metricsTracker.getInbound(),
                        api,
                        new ErrorHandler());
                }
            };

            ChannelFuture bindFuture = new ServerBootstrap()
                .group(listen.get(), work.get())
                .childHandler(childHandler)
                .channel(NioServerSocketChannel.class)
                .option(CONNECT_TIMEOUT_MILLIS, 10_000)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .bind(port)
                .addListener(future ->
                    log.info("{}: Bound to port {}", this, port));

            try {
                return bindFuture.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted", e);
            }
        });
    }

    public void start() {
        log.info(this + " started: " + started.get());
    }

    public void stop() {
        Get.ifPresent(started, channelFuture -> {
            try {
                Optional.ofNullable(channelFuture)
                    .map(ChannelFuture::channel)
                    .map(Channel::close)
                    .map(closedFuture ->
                        closedFuture.addListener(future ->
                            log.info("{}: closed: {}", this, future)));
            } finally {
                try {
                    Get.ifPresent(listen, EventLoopGroup::shutdownGracefully);
                } finally {
                    Get.ifPresent(work, EventLoopGroup::shutdownGracefully);
                }
            }
        });
    }

    private static final int MAX_CONTENT_LENGTH = 16 * 1024 * 1026;

    private static final String METRICS_PATH = "/metrics/";

    private static final String HEALTH_PATH = "/health/";

    private static ThreadFactory countingThreadFactory() {
        LongAdder count = new LongAdder();
        return runnable -> {
            try {
                return new Thread(runnable, "t" + count.longValue());
            } finally {
                count.increment();
            }
        };
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[]";
    }
}
