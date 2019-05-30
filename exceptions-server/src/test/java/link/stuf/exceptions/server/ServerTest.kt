package link.stuf.exceptions.server

import link.stuf.exceptions.server.api.WiredException
import org.http4k.client.ApacheClient
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.format.Jackson
import org.http4k.format.Jackson.auto

fun main() {

    val server = WiredExceptionsServer(9000)

    val lens = Body.auto<WiredException>().toLens()

    val input = "java.lang.IllegalStateException: Errr\n" +
            "\tat link.stuf.exceptions.core.inputs.ThrowableParserTest.andFail(ThrowableParserTest.java:43)\n" +
            "\tat link.stuf.exceptions.core.inputs.ThrowableParserTest.parseSimple(ThrowableParserTest.java:18)\n" +
            "\tat java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n" +
            "\tat java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n" +
            "\tat java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n" +
            "\tat java.base/java.lang.reflect.Method.invoke(Method.java:567)\n" +
            "\tat org.junit.runners.model.FrameworkMethod\$1.runReflectiveCall(FrameworkMethod.java:50)\n" +
            "\tat org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)\n" +
            "\tat org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)\n" +
            "\tat org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)\n" +
            "\tat org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)\n" +
            "\tat org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:78)\n" +
            "\tat org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:57)\n" +
            "\tat org.junit.runners.ParentRunner\$3.run(ParentRunner.java:290)\n" +
            "\tat org.junit.runners.ParentRunner\$1.schedule(ParentRunner.java:71)\n" +
            "\tat org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)\n" +
            "\tat org.junit.runners.ParentRunner.access\$000(ParentRunner.java:58)\n" +
            "\tat org.junit.runners.ParentRunner\$2.evaluate(ParentRunner.java:268)\n" +
            "\tat org.junit.runners.ParentRunner.run(ParentRunner.java:363)\n" +
            "\tat org.gradle.api.internal.tasks.testing.junit.JUnitTestClassExecutor.runTestClass(JUnitTestClassExecutor.java:110)\n" +
            "\tat org.gradle.api.internal.tasks.testing.junit.JUnitTestClassExecutor.execute(JUnitTestClassExecutor.java:58)\n" +
            "\tat org.gradle.api.internal.tasks.testing.junit.JUnitTestClassExecutor.execute(JUnitTestClassExecutor.java:38)\n" +
            "\tat org.gradle.api.internal.tasks.testing.junit.AbstractJUnitTestClassProcessor.processTestClass(AbstractJUnitTestClassProcessor.java:62)\n" +
            "\tat org.gradle.api.internal.tasks.testing.SuiteTestClassProcessor.processTestClass(SuiteTestClassProcessor.java:51)\n" +
            "\tat java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n" +
            "\tat java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n" +
            "\tat java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n" +
            "\tat java.base/java.lang.reflect.Method.invoke(Method.java:567)\n" +
            "\tat gradle@foo.bar.3/org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:35)\n" +
            "\tat gradle@foo.bar.3/org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:24)\n" +
            "\tat gradle@foo.bar.3/org.gradle.internal.dispatch.ContextClassLoaderDispatch.dispatch(ContextClassLoaderDispatch.java:32)\n" +
            "\tat gradle@foo.bar.3/org.gradle.internal.dispatch.ProxyDispatchAdapter\$DispatchingInvocationHandler.invoke(ProxyDispatchAdapter.java:93)\n" +
            "\tat com.sun.proxy.\$Proxy2.processTestClass(Unknown Source)\n" +
            "\tat org.gradle.api.internal.tasks.testing.worker.TestWorker.processTestClass(TestWorker.java:118)\n" +
            "\tat java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n" +
            "\tat java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n" +
            "\tat java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n" +
            "\tat java.base/java.lang.reflect.Method.invoke(Method.java:567)\n" +
            "\tat org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:35)\n" +
            "\tat org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:24)\n" +
            "\tat org.gradle.internal.remote.internal.hub.MessageHubBackedObjectConnection\$DispatchWrapper.dispatch(MessageHubBackedObjectConnection.java:175)\n" +
            "\tat org.gradle.internal.remote.internal.hub.MessageHubBackedObjectConnection\$DispatchWrapper.dispatch(MessageHubBackedObjectConnection.java:157)\n" +
            "\tat org.gradle.internal.remote.internal.hub.MessageHub\$Handler.run(MessageHub.java:404)\n" +
            "\tat org.gradle.internal.concurrent.ExecutorPolicy\$CatchAndRecordFailures.onExecute(ExecutorPolicy.java:63)\n" +
            "\tat org.gradle.internal.concurrent.ManagedExecutorImpl\$1.run(ManagedExecutorImpl.java:46)\n" +
            "\tat java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)\n" +
            "\tat java.base/java.util.concurrent.ThreadPoolExecutor\$Worker.run(ThreadPoolExecutor.java:628)\n" +
            "\tat org.gradle.internal.concurrent.ThreadFactoryImpl\$ManagedThreadRunnable.run(ThreadFactoryImpl.java:55)\n" +
            "\tat java.base/java.lang.Thread.run(Thread.java:835)\n" +
            "Caused by: java.lang.IllegalStateException: Argh!\n" +
            "\tat link.stuf.exceptions.core.inputs.ThrowableParserTest.andFailAgain(ThrowableParserTest.java:48)\n" +
            "\tat link.stuf.exceptions.core.inputs.ThrowableParserTest.andFail(ThrowableParserTest.java:41)\n"

    val client = ApacheClient()

    val uuid = client(Request(Method.POST, "http://localhost:9000/submit").body(input))

    println(uuid.bodyString())
    println(client(Request(Method.POST, "http://localhost:9000/echo").body(input)).bodyString())

    val target = client(Request(Method.GET, "http://localhost:9000/lookup/" + uuid.bodyString()))

    val exc = lens.extract(target)

    println(Jackson.prettify(Jackson.asJsonString(exc)))

    server.stop()
}
