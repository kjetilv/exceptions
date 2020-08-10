package unearth.netty;

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
import unearth.server.UnearthlyServer;

import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;
import static java.lang.StrictMath.toIntExact;

@SuppressWarnings("SameParameterValue")
final class NettyServer<A> implements UnearthlyServer {
    
    private static final Logger log = LoggerFactory.getLogger(NettyServer.class);
    
    private final int listenThreads;
    
    private final int workThreads;
    
    private final int threads;
    
    private final int queue;
    
    private final Duration connectTimeout;
    
    private final int port;
    
    private final EventLoopGroup listen;
    
    private final EventLoopGroup work;
    
    private final AtomicReference<ChannelFuture> future = new AtomicReference<>();
    
    private final AtomicBoolean closed = new AtomicBoolean();
    
    private final List<ChannelHandler> handlers;
    
    NettyServer(int port, ChannelHandler... handlers) {
        this(port, Arrays.asList(handlers));
    }
    
    NettyServer(int port, List<ChannelHandler> handlers) {
        this.port = port;
        
        this.handlers = List.copyOf(handlers);
        
        this.listenThreads = Runtime.getRuntime().availableProcessors();
        this.workThreads = 10;
        this.threads = 32;
        this.queue = 32;
        this.connectTimeout = Duration.ofSeconds(10);
        
        this.listen = listenGroup();
        this.work = workGroup();
    }
    
    @Override
    public void start() {
        future.updateAndGet(fucheer -> fucheer != null
            ? fucheer
            : new ServerBootstrap()
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    
                    @Override
                    protected void initChannel(NioSocketChannel channel) {
                        ChannelPipeline pipeline = channel.pipeline()
                            .addLast("decoder", new HttpServerCodec())
                            .addLast("aggregator", new HttpObjectAggregator(MAX_CONTENT_LENGTH));
                        handlers.forEach(handler ->
                            pipeline.addLast(handler.getClass().getSimpleName(), handler));
                    }
                })
                .channel(NioServerSocketChannel.class)
                .group(listen, work)
                .option(CONNECT_TIMEOUT_MILLIS, toIntExact(connectTimeout.toMillis()))
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .bind(port)
                .addListener(future -> {
                    log.info("{}: Bound to port {}", this, port);
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(), "Closer"));
                }));
    }
    
    @Override
    public void stop() {
        if (closed.compareAndSet(false, true)) {
            shutdown();
        }
    }
    
    @Override
    public void close() {
        stop();
    }
    
    @Override
    public int port() {
        return port;
    }
    
    private void shutdown() {
        try {
            future.get().channel().close()
                .addListener(future ->
                    log.info("{}: closed {}: {}", this, port, future));
        } finally {
            listen.shutdownGracefully();
            work.shutdownGracefully();
        }
    }
    
    private EventLoopGroup listenGroup() {
        return new NioEventLoopGroup(this.listenThreads, countingThreadFactory("lst"));
    }
    
    private EventLoopGroup workGroup() {
        return new NioEventLoopGroup(this.workThreads, new ThreadPoolExecutor(
            threads,
            threads,
            60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(queue),
            countingThreadFactory("thr"),
            new ThreadPoolExecutor.CallerRunsPolicy()));
    }
    
    private static final int MAX_CONTENT_LENGTH = 16 * 1024 * 1026;
    
    private static ThreadFactory countingThreadFactory(String prefix) {
        LongAdder count = new LongAdder();
        return r -> {
            try {
                return new Thread(r, prefix + '#' + count.longValue());
            } finally {
                count.increment();
            }
        };
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" +
            "l:" + listenThreads + "/w:" + workThreads + " -> t:" + threads + "/q:" + queue +
            "]";
    }
}
