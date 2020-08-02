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
package unearth.munch.parser;

import java.io.ByteArrayOutputStream;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.util.regex.Pattern;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class ThrowableParserTest {
    
    @Test
    public void parseSuppressed() {
        try {
            andFailSuppressed();
        } catch (MyRuntimeException e) {
            String output = print(e);
            System.out.println(output);
            Throwable chameleon = ThrowableParser.parse(output);
            assertNotNull(chameleon);
            String print = print(chameleon);
            System.out.println("\n\n" + print);
        }
    }
    
    @Test
    public void parseSuppressed2() {
        Throwable chameleon = ThrowableParser.parse(SUPP);
        assertNotNull(chameleon);
        String print = print(chameleon);
        System.out.println("\n\n" + print);
    }
    
    @Test
    public void parseConflated() {
        
        try {
            andFail();
        } catch (Exception e) {
            String output = conflate(print(e));
            System.out.println(output);
            Throwable chameleon = ThrowableParser.parse(output);
            assertNotNull(chameleon);
            String print = print(chameleon);
            System.out.println(output);
            System.out.println(print);
        }
    }
    
    @Test
    public void parseSimple() {
        
        try {
            andFail();
        } catch (Exception e) {
            String output = print(e);
            System.out.println(output);
            Throwable chameleon = ThrowableParser.parse(output);
            assertNotNull(chameleon);
            String print = print(chameleon);
            System.out.println(output);
            System.out.println(print);
        }
    }
    
    private static final String SUPP =
        """
        Exception in thread "main" java.lang.IllegalStateException: Failed to perform action
        	at unearth.analysis.AbstractCassandraConnected.inSession(AbstractCassandraConnected.java:83)
        	at unearth.analysis.CassandraInit.init(CassandraInit.java:30)
        	at unearth.server.Unearth.run(Unearth.kt:63)
        	at unearth.main.MainKt.main(Main.kt:26)
        	at unearth.main.MainKt.main(Main.kt)
        Caused by: java.lang.IllegalStateException: GetOnce[unearth.analysis.AbstractCassandraConnected$$Lambda$55/0x0000000800bef680@591e58fa]: failed
        	at unearth.util.once.GetOnce.failed(GetOnce.java:77)
        	at unearth.util.once.GetOnce.initial(GetOnce.java:56)
        	at unearth.util.once.GetOnce.doGet(GetOnce.java:46)
        	at unearth.util.once.AbstractGet.get(AbstractGet.java:34)
        	at unearth.analysis.AbstractCassandraConnected.inSession(AbstractCassandraConnected.java:81)
        	... 4 more
        Caused by: com.datastax.oss.driver.api.core.AllNodesFailedException: Could not reach any contact point, make sure you've provided valid addresses (showing first 1 nodes, use getAllErrors() for more): Node(endPoint=127.0.0.1/<unresolved>:9042, hostId=null, hashCode=76b94a8c): [com.datastax.oss.driver.api.core.connection.ConnectionInitException: [s0|control|connecting...] Protocol initialization request, step 1 (OPTIONS): failed to send request (java.nio.channels.ClosedChannelException)]
        	at com.datastax.oss.driver.api.core.AllNodesFailedException.copy(AllNodesFailedException.java:141)
        	at com.datastax.oss.driver.internal.core.util.concurrent.CompletableFutures.getUninterruptibly(CompletableFutures.java:149)
        	at com.datastax.oss.driver.api.core.session.SessionBuilder.build(SessionBuilder.java:633)
        	at unearth.analysis.AbstractCassandraConnected.lambda$new$0(AbstractCassandraConnected.java:55)
        	at unearth.util.once.GetOnce.initial(GetOnce.java:54)
        	... 7 more
        	Suppressed: com.datastax.oss.driver.api.core.connection.ConnectionInitException: [s0|control|connecting...] Protocol initialization request, step 1 (OPTIONS): failed to send request (java.nio.channels.ClosedChannelException)
        		at com.datastax.oss.driver.internal.core.channel.ProtocolInitHandler$InitRequest.fail(ProtocolInitHandler.java:342)
        		at com.datastax.oss.driver.internal.core.channel.ChannelHandlerRequest.writeListener(ChannelHandlerRequest.java:87)
        		at io.netty.util.concurrent.DefaultPromise.notifyListener0(DefaultPromise.java:577)
        		at io.netty.util.concurrent.DefaultPromise.notifyListenersNow(DefaultPromise.java:551)
        		at io.netty.util.concurrent.DefaultPromise.notifyListeners(DefaultPromise.java:490)
        		at io.netty.util.concurrent.DefaultPromise.addListener(DefaultPromise.java:183)
        		at io.netty.channel.DefaultChannelPromise.addListener(DefaultChannelPromise.java:95)
        		at io.netty.channel.DefaultChannelPromise.addListener(DefaultChannelPromise.java:30)
        		at com.datastax.oss.driver.internal.core.channel.ChannelHandlerRequest.send(ChannelHandlerRequest.java:76)
        		at com.datastax.oss.driver.internal.core.channel.ProtocolInitHandler$InitRequest.send(ProtocolInitHandler.java:183)
        		at com.datastax.oss.driver.internal.core.channel.ProtocolInitHandler.onRealConnect(ProtocolInitHandler.java:118)
        		at com.datastax.oss.driver.internal.core.channel.ConnectInitHandler.lambda$connect$0(ConnectInitHandler.java:57)
        		at io.netty.util.concurrent.DefaultPromise.notifyListener0(DefaultPromise.java:577)
        		at io.netty.util.concurrent.DefaultPromise.notifyListeners0(DefaultPromise.java:570)
        		at io.netty.util.concurrent.DefaultPromise.notifyListenersNow(DefaultPromise.java:549)
        		at io.netty.util.concurrent.DefaultPromise.notifyListeners(DefaultPromise.java:490)
        		at io.netty.util.concurrent.DefaultPromise.setValue0(DefaultPromise.java:615)
        		at io.netty.util.concurrent.DefaultPromise.setFailure0(DefaultPromise.java:608)
        		at io.netty.util.concurrent.DefaultPromise.tryFailure(DefaultPromise.java:117)
        		at io.netty.channel.nio.AbstractNioChannel$AbstractNioUnsafe.fulfillConnectPromise(AbstractNioChannel.java:321)
        		at io.netty.channel.nio.AbstractNioChannel$AbstractNioUnsafe.finishConnect(AbstractNioChannel.java:337)
        		at io.netty.channel.nio.NioEventLoop.processSelectedKey(NioEventLoop.java:702)
        		at io.netty.channel.nio.NioEventLoop.processSelectedKeysOptimized(NioEventLoop.java:650)
        		at io.netty.channel.nio.NioEventLoop.processSelectedKeys(NioEventLoop.java:576)
        		at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:493)
        		at io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:989)
        		at io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
        		at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
        		at java.base/java.lang.Thread.run(Thread.java:832)
        		Suppressed: io.netty.channel.AbstractChannel$AnnotatedConnectException: Connection refused: /127.0.0.1:9042
        		Caused by: java.net.ConnectException: Connection refused
        			at java.base/sun.nio.ch.Net.pollConnect(Native Method)
        			at java.base/sun.nio.ch.Net.pollConnectNow(Net.java:658)
        			at java.base/sun.nio.ch.SocketChannelImpl.finishConnect(SocketChannelImpl.java:875)
        			at io.netty.channel.socket.nio.NioSocketChannel.doFinishConnect(NioSocketChannel.java:330)
        			at io.netty.channel.nio.AbstractNioChannel$AbstractNioUnsafe.finishConnect(AbstractNioChannel.java:334)
        			at io.netty.channel.nio.NioEventLoop.processSelectedKey(NioEventLoop.java:702)
        			at io.netty.channel.nio.NioEventLoop.processSelectedKeysOptimized(NioEventLoop.java:650)
        			at io.netty.channel.nio.NioEventLoop.processSelectedKeys(NioEventLoop.java:576)
        			at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:493)
        			at io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:989)
        			at io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
        			at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
        			at java.base/java.lang.Thread.run(Thread.java:832)
        	Caused by: java.nio.channels.ClosedChannelException
        		at io.netty.channel.AbstractChannel$AbstractUnsafe.newClosedChannelException(AbstractChannel.java:957)
        		at io.netty.channel.AbstractChannel$AbstractUnsafe.flush0(AbstractChannel.java:921)
        		at io.netty.channel.nio.AbstractNioChannel$AbstractNioUnsafe.flush0(AbstractNioChannel.java:354)
        		at io.netty.channel.AbstractChannel$AbstractUnsafe.flush(AbstractChannel.java:897)
        		at io.netty.channel.DefaultChannelPipeline$HeadContext.flush(DefaultChannelPipeline.java:1372)
        		at io.netty.channel.AbstractChannelHandlerContext.invokeFlush0(AbstractChannelHandlerContext.java:750)
        		at io.netty.channel.AbstractChannelHandlerContext.invokeFlush(AbstractChannelHandlerContext.java:742)
        		at io.netty.channel.AbstractChannelHandlerContext.flush(AbstractChannelHandlerContext.java:728)
        		at io.netty.channel.ChannelDuplexHandler.flush(ChannelDuplexHandler.java:127)
        		at io.netty.channel.AbstractChannelHandlerContext.invokeFlush0(AbstractChannelHandlerContext.java:750)
        		at io.netty.channel.AbstractChannelHandlerContext.invokeWriteAndFlush(AbstractChannelHandlerContext.java:765)
        		at io.netty.channel.AbstractChannelHandlerContext.write(AbstractChannelHandlerContext.java:790)
        		at io.netty.channel.AbstractChannelHandlerContext.writeAndFlush(AbstractChannelHandlerContext.java:758)
        		at io.netty.channel.AbstractChannelHandlerContext.writeAndFlush(AbstractChannelHandlerContext.java:808)
        		at io.netty.channel.DefaultChannelPipeline.writeAndFlush(DefaultChannelPipeline.java:1025)
        		at io.netty.channel.AbstractChannel.writeAndFlush(AbstractChannel.java:294)
        		at com.datastax.oss.driver.internal.core.channel.ChannelHandlerRequest.send(ChannelHandlerRequest.java:75)
        		... 20 more
        """;
    private static final Pattern WS = Pattern.compile("\\s+");
    
    private static final String S = " ";
    
    private static String conflate(String ePrint) {
        
        return WS.matcher(String.join(S, ePrint.split("\n")))
            .replaceAll(S);
    }
    
    private static String print(Throwable e) {
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter pw = new PrintWriter(out)) {
            e.printStackTrace(pw);
        }
        return new String(out.toByteArray());
    }
    
    private static void andFail() {
        try {
            andFailAgain();
        } catch (Exception e) {
            throw new IllegalStateException("Errr", e);
        }
    }
    
    private static void andFailSuppressed() {
        try {
            andFailAgainSuppressed();
        } catch (MyRuntimeException e) {
            MyRuntimeException errr = new MyRuntimeException("Errr", e);
            errr.addSuppressed(new IllegalStateException("Aff", new InterruptedIOException("Int")));
            errr.addSuppressed(new IllegalArgumentException("Uff", new NumberFormatException("Ans42")));
            throw errr;
        }
    }
    
    private static void andFailAgain() {
        throw new IllegalStateException("Argh!");
    }
    
    private static void andFailAgainSuppressed() {
        MyRuntimeException exception = new MyRuntimeException();
        exception.addSuppressed(new ConnectException("1.0.0.1"));
        throw exception;
    }
    
    private static class MyRuntimeException extends RuntimeException {
        
        private MyRuntimeException() {
            super("Argh!", null, true, true);
        }
        
        private MyRuntimeException(String errr, MyRuntimeException e) {
            super(errr, e, true, true);
        }
    }
}
