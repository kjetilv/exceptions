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
 *     along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package no.scienta.unearth.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import no.scienta.unearth.dto.Submission;
import no.scienta.unearth.munch.json.IdModule;

import java.io.InputStream;
import java.net.CookieHandler;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Function;

import static java.net.http.HttpClient.Redirect.NEVER;
import static java.net.http.HttpClient.Version.HTTP_1_1;
import static java.util.concurrent.TimeUnit.SECONDS;

@SuppressWarnings("WeakerAccess")
public class UnearthClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient client;

    private final URI uri;

    private final ObjectMapper objectMapper;

    public static UnearthClient connect(URI uri) {
        return new UnearthClient(uri);
    }

    public UnearthClient(URI uri) {
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
            .registerModule(new Jdk8Module())
            .registerModule(new IdModule().addDefaults());
    }

    public String print(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to write: " + obj, e);
        }
    }

    public Submission submit(InputStream inputStream) {
        return publish(new ThrowablePublisher(inputStream));
    }

    public Submission submit(Throwable throwable) {
        return publish(new ThrowablePublisher(throwable));
    }

    private Submission publish(ThrowablePublisher throwablePublisher) {
        try {
            Function<HttpResponse<String>, Submission> httpResponseObjectFunction = (HttpResponse<String> res) -> {
                try {
                    return objectMapper.readerFor(Submission.class).readValue(res.body());
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to read " + Submission.class, e);
                }
            };
            return client.sendAsync(
                HttpRequest.newBuilder().POST(throwablePublisher)
                    .timeout(TIMEOUT)
                    .uri(uri)
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            ).thenApply(httpResponseObjectFunction).get();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish", e);
        }
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
}
