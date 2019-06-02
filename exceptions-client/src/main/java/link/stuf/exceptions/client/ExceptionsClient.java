package link.stuf.exceptions.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import link.stuf.exceptions.dto.Submission;
import link.stuf.exceptions.munch.Throwables;

import java.net.CookieHandler;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.ThreadPoolExecutor;

import static java.net.http.HttpClient.Redirect.NEVER;
import static java.net.http.HttpClient.Version.HTTP_1_1;
import static java.util.concurrent.TimeUnit.SECONDS;

public class ExceptionsClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient client;

    private final URI uri;

    private final ObjectMapper objectMapper;

    static ExceptionsClient connect(int port) {
        return new ExceptionsClient(port);
    }

    static ExceptionsClient connect(URI uri) {
        return new ExceptionsClient(uri);
    }

    private ExceptionsClient(int port) {
        this(URI.create("http://localhost:" + port));
    }

    private ExceptionsClient(URI uri) {
        this.uri = Objects.requireNonNull(uri);
        this.client = HttpClient.newBuilder()
            .followRedirects(NEVER)
            .cookieHandler(new NoCookieHandler())
            .executor(new ThreadPoolExecutor(
                10,
                20,
                30, SECONDS,
                new ArrayBlockingQueue<>(10)))
            .connectTimeout(TIMEOUT)
            .version(HTTP_1_1)
            .build();
        this.objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .registerModule(new KotlinModule())
            .registerModule(new Jdk8Module());
    }

    CompletableFuture<Submission> submit(Throwable throwable) {
        return client.sendAsync(
            HttpRequest.newBuilder().POST(new ThrowablePublisher(throwable))
                .timeout(TIMEOUT)
                .uri(uri)
                .build(),
            HttpResponse.BodyHandlers.ofString()
        ).thenApply((HttpResponse<String> res) -> {
            try {
                return objectMapper.readerFor(Submission.class).readValue(res.body());
            } catch (Exception e) {
                throw new IllegalStateException("Failed to read " + Submission.class, e);
            }
        });
    }

    private static class NoCookieHandler extends CookieHandler {
        @Override
        public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders) {
            return Collections.emptyMap();
        }

        @Override
        public void put(URI uri, Map<String, List<String>> responseHeaders) {

        }
    }

    private static class ThrowablePublisher implements HttpRequest.BodyPublisher {

        private final Throwable throwable;

        private final ByteBuffer bytes;

        ThrowablePublisher(Throwable throwable) {
            this.throwable = throwable;
            this.bytes = Throwables.byteBuffer(throwable);
        }

        @Override
        public long contentLength() {
            return bytes.limit();
        }

        @Override
        public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
            subscriber.onNext(bytes);
        }
    }
}
