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

package unearth.main.netty

import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import unearth.server.UnearthlyConfig
import unearth.server.UnearthlyController
import unearth.server.UnearthlyServer

class NettyServer(
    private val controller: UnearthlyController,
    private val configuration: UnearthlyConfig = UnearthlyConfig()
) : UnearthlyServer {

    companion object {
        val log: Logger = LoggerFactory.getLogger(NettyServer::class.java)
    }

    override fun start(after: (UnearthlyServer) -> Unit): UnearthlyServer {
        val listenGroup = NioEventLoopGroup(Runtime.getRuntime().availableProcessors())
        val workGroup = NioEventLoopGroup(20)
        val opened = sync(future(Router(), listenGroup, workGroup), "sdf")
        registerShutdownClosure(opened)
        val closed: ChannelFuture = sync(
            opened.channel().closeFuture(),
            "Interrupted while closing")
        return this
    }

    override fun stop(after: (UnearthlyServer) -> Unit): UnearthlyServer {
        TODO("Not yet implemented")
    }

    private fun future(
        router: Router,
        listenGroup: EventLoopGroup,
        workGroup: EventLoopGroup
    ): ChannelFuture {
        return io.netty.bootstrap.ServerBootstrap()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, StrictMath.toIntExact(configuration.connectTimeout.toMillis()))
            .group(listenGroup, workGroup)
            .channel(io.netty.channel.socket.nio.NioServerSocketChannel::class.java)
            .handler(io.netty.handler.logging.LoggingHandler())
            .childHandler(ChannelInit(router))
            .bind(configuration.port)
    }

    private fun registerShutdownClosure(sync: ChannelFuture) {
        Runtime.getRuntime().addShutdownHook(Thread({
            log.info("{}: Closing {} ...", this, sync.channel())
            sync.channel().close()
        }, "Closer"))
    }

    private fun sync(future: ChannelFuture, msg: String): ChannelFuture =
        try {
            future.sync()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IllegalStateException("$msg: $future", e)
        }

    override fun port(): Int = configuration.port

    override fun reset() {
        TODO("Not yet implemented")
    }
}

internal class ChannelInit(private val router: Router) : ChannelInitializer<NioSocketChannel>() {

    override fun initChannel(ch: NioSocketChannel) {
        ch.pipeline()
            .addLast("decoder", HttpServerCodec())
            .addLast("aggregator", HttpObjectAggregator(4096))
            .addLast(router)
    }

}

class Router: SimpleChannelInboundHandler<FullHttpRequest>() {
    override fun channelRead0(ctx: ChannelHandlerContext?, msg: FullHttpRequest?) {
        TODO("Not yet implemented")
    }
}
