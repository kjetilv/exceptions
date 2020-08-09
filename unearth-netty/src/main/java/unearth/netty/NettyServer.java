package unearth.netty;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
import unearth.norest.common.IOHandler;
import unearth.norest.server.ForwardableMethods;
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
    
    private final A impl;
    
    private final int port;
    
    private final ForwardableMethods<A> invokableMethods;
    
    private final IOHandler ioHandler;
    
    private final EventLoopGroup listen;
    
    private final EventLoopGroup work;
    
    private ChannelFuture future;
    
    private final AtomicBoolean closed = new AtomicBoolean();
    
    NettyServer(UnearthlyConfig config, A impl, ForwardableMethods<A> invokableMethods, IOHandler ioHandler) {
        this.impl = impl;
        this.port = config.getPort();
        
        this.invokableMethods = invokableMethods;
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
        ChannelFuture bind = new ServerBootstrap()
            .option(CONNECT_TIMEOUT_MILLIS, toIntExact(connectTimeout.toMillis()))
            .group(listen, work)
            .channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler(LogLevel.DEBUG))
            .childHandler(new ChannelInitializer<NioSocketChannel>() {
                
                @Override
                protected void initChannel(NioSocketChannel ch) {
                    ch.pipeline()
                        .addLast("decoder", new HttpServerCodec())
                        .addLast("aggregator", new HttpObjectAggregator(MAX_CONTENT_LENGTH))
                        .addLast(new ApiRouter<>(impl, invokableMethods, ioHandler));
                }
            })
            .bind(port);
        future = sync(bind, "Interrupted while starting");
        log.info("{}: Bound to port {}: {}", this, port, future.channel());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(null), "Closer"));
        return this;
    }
    
    @Override
    public NettyServer<A> stop(Consumer<UnearthlyServer> after) {
        try {
            if (closed.compareAndSet(false, true)) {
                shutdown();
            } else {
                if (after != null) {
                    log.info("{} already shut down", this);
                } else {
                    log.debug("{} already shut down", this);
                }
            }
        } finally {
            if (after != null) {
                try {
                    after.accept(this);
                } catch (Exception e) {
                    log.error(this + ": Failure after close", e);
                }
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
    
    private void shutdown() {
        try {
            ChannelFuture closed = sync(
                this.future.channel().close(),
                "Interrupted while closing");
            log.info("{}: closed {}: {}", this, port, closed);
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
    
    private static ChannelFuture sync(ChannelFuture future, String msg) {
        try {
            return future.sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(msg + ": " + future, e);
        }
    }
    
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
