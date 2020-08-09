package unearth.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

final class ChannelInit extends ChannelInitializer<NioSocketChannel> {

    private final ChannelHandler router;
    
    private final int chunkSize;
    
    ChannelInit(ChannelHandler router, int chunkSize) {
        this.router = router;
        this.chunkSize = chunkSize;
    }

    @Override
    protected void initChannel(NioSocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline
            .addLast("decoder", new HttpServerCodec())
            .addLast("aggregator", new HttpObjectAggregator(chunkSize))
            .addLast(router);
    }
}
