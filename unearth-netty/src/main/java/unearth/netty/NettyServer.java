package unearth.netty;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
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
import unearth.norest.ApiInvoker;
import unearth.norest.common.IOHandler;
import unearth.server.UnearthlyConfig;
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
    
    private final ApiInvoker<A> apiInvoker;
    
    private final IOHandler ioHandler;
    
    private final EventLoopGroup listen;
    
    private final EventLoopGroup work;
    
    private final AtomicReference<ChannelFuture> future = new AtomicReference<ChannelFuture>();
    
    private final AtomicBoolean closed = new AtomicBoolean();
    
    NettyServer(UnearthlyConfig config, ApiInvoker<A> apiInvoker, IOHandler ioHandler) {
        this.port = config.getPort();
        this.apiInvoker = apiInvoker;
        
        this.ioHandler = ioHandler;
        
        this.listenThreads = Runtime.getRuntime().availableProcessors();
        this.workThreads = 10;
        this.threads = 32;
        this.queue = 32;
        this.connectTimeout = Duration.ofSeconds(10);
        
        this.listen = listenGroup();
        this.work = workGroup();
    }
    
    @Override
    public UnearthlyServer start(Consumer<UnearthlyServer> after) {
        future.updateAndGet(fucheer -> fucheer != null
            ? fucheer
            : new ServerBootstrap()
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    
                    @Override
                    protected void initChannel(NioSocketChannel channel) {
                        channel.pipeline()
                            .addLast("decoder", new HttpServerCodec())
                            .addLast("aggregator", new HttpObjectAggregator(MAX_CONTENT_LENGTH))
                            .addLast("apiInvoker", new ApiRouter<>(ioHandler, apiInvoker));
                    }
                })
                .channel(NioServerSocketChannel.class)
                .group(listen, work)
                .option(CONNECT_TIMEOUT_MILLIS, toIntExact(connectTimeout.toMillis()))
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .bind(port)
                .addListener(future -> {
                    log.info("{}: Bound to port {}", this, port);
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(null), "Closer"));
                })
                .addListener(future -> {
                    if (after != null) {
                        after.accept(this);
                    }
                }));
        return this;
    }
    
    @Override
    public NettyServer<A> stop(Consumer<UnearthlyServer> after) {
        if (closed.compareAndSet(false, true)) {
            shutdown().addListener(future -> {
                if (after != null) {
                    try {
                        after.accept(this);
                    } catch (Exception e) {
                        log.error(this + ": Failure after close", e);
                    }
                }
            });
        } else {
            if (after != null) {
                log.info("{} already shut down", this);
            } else {
                log.debug("{} already shut down", this);
            }
        }
        return this;
    }
    
    @Override
    public void close() {
        stop(null);
    }
    
    @Override
    public int port() {
        return port;
    }
    
    @Override
    public void reset() {
    }
    
    private ChannelFuture shutdown() {
        try {
            return future.get().channel().close()
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
