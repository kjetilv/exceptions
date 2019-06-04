package link.stuf.exceptions.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import org.http4k.core.HttpHandler
import org.http4k.server.Http4kChannelHandler
import org.http4k.server.Http4kServer
import org.http4k.server.ServerConfig
import java.net.InetSocketAddress

data class NettyConfig(
        val host: String = "0.0.0.0",
        val port: Int = 8000
) : ServerConfig {

    override fun toServer(httpHandler: HttpHandler) = object : Http4kServer {

        private val masterGroup = NioEventLoopGroup()

        private val workerGroup = NioEventLoopGroup()

        private var closeFuture: ChannelFuture? = null

        private lateinit var address: InetSocketAddress

        override fun start(): Http4kServer = apply {
            val bootstrap = ServerBootstrap()
            bootstrap.group(masterGroup, workerGroup)
                    .channelFactory(ChannelFactory<ServerChannel> { NioServerSocketChannel() })
                    .childHandler(object : ChannelInitializer<SocketChannel>() {
                        public override fun initChannel(ch: SocketChannel) {
                            ch.pipeline().addLast("codec", HttpServerCodec())
                            ch.pipeline().addLast("aggregator", HttpObjectAggregator(Int.MAX_VALUE))
                            ch.pipeline().addLast("handler", Http4kChannelHandler(httpHandler))
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 1000)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)

            val channel = bootstrap.bind(InetSocketAddress(host, port)).sync().channel()
            address = channel.localAddress() as InetSocketAddress
            closeFuture = channel.closeFuture()
        }

        override fun stop() = apply {
            closeFuture?.cancel(false)
            workerGroup.shutdownGracefully()
            masterGroup.shutdownGracefully()
        }

        override fun port(): Int = if (port > 0) 0 else address.port
    }
}
