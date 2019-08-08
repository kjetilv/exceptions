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

package no.scienta.unearth.client.proto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

class Invoker implements InvocationHandler {

    private final URI uri;

    private final ObjectMapper objectMapper;

    private final Map<Method, Meta> metas = new HashMap<>();

    private final Function<Object, byte[]> writeBytes = this::writeBytes;

    private final BiFunction<Class<?>, InputStream, Object> readBytes = this::readBytes;

    Invoker(URI uri, ObjectMapper objectMapper) {
        this.uri = Objects.requireNonNull(uri, "uri").toASCIIString().endsWith("/")
            ? uri
            : URI.create(uri.toASCIIString() + "/");
        this.objectMapper = objectMapper;
        if (!this.uri.toASCIIString().endsWith("/")) {
            throw new IllegalArgumentException("Expected slashed URI: " + uri);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        Meta meta = meta(method);
        HttpRequest request = request(meta, args);
        if (meta.noResponse()) {
            HttpResponse<InputStream> response = response(request);
            failOnError(response);
            return null;
        }
        HttpResponse<InputStream> response = response(request);
        if (response.statusCode() == 204 && meta.optional()) {
            return Optional.empty();
        }
        failOnError(response);
        Object object = readResponse(meta, response);
        return returnValue(meta, object);
    }

    private Object returnValue(Meta meta, Object object) {
        if (object != null) {
            return meta.optional() ? Optional.of(object) : object;
        }
        if (meta.optional()) {
            return Optional.empty();
        }
        throw new IllegalStateException("No object returned");
    }

    private Meta meta(Method method) {
        return metas.computeIfAbsent(method, __ -> new Meta(method, writeBytes, readBytes));
    }

    private HttpRequest request(Meta meta, Object[] args) {
        return withBody(meta, HttpRequest.newBuilder().uri(uri(meta, args)), args)
            .header(CONTENT_TYPE, meta.contentType())
            .build();
    }

    private HttpResponse<InputStream> response(HttpRequest request) {
        try {
            return newClient().send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send " + request, e);
        }
    }

    private HttpResponse<Void> voidResponse(HttpRequest request) {
        try {
            return newClient().send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send " + request, e);
        }
    }

    private byte[] writeBytes(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to write body: " + value, e);
        }
    }

    private Object readBytes(Class<?> type, InputStream inputStream) {
        try {
            return objectMapper.readerFor(type).readValue(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read response: " + type + " <= " + inputStream, e);
        }
    }

    private URI uri(Meta meta, Object[] args) {
        return URI.create(this.uri.toASCIIString() + meta.path(args));
    }

    private HttpClient newClient() {
        return HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    private void failOnError(HttpResponse<InputStream> response) {
        int result = response.statusCode();
        if (result >= 500) {
            throw new IllegalStateException("Internal server error: " + response + possibly(error(response)));
        }
        if (result >= 400) {
            throw new IllegalArgumentException("Invalid request: " + response + possibly(error(response)));
        }
        if (result >= 300) {
            throw new IllegalArgumentException("Unsupported redirect: " + response + possibly(error(response)));
        }
    }

    private String possibly(String error) {
        return error == null || error.trim().isEmpty() ? "" : "\n" + error.trim();
    }

    private String error(HttpResponse<InputStream> response) {
        try (BufferedReader reader =
                 new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))
        ) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            return "<Failed to read error response: " + e + ">";
        }
    }

    private Object readResponse(Meta meta, HttpResponse<InputStream> response) {
        try (InputStream body = response.body()) {
            return meta.response(body);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read response: " + response, e);
        }
    }

    private static final String CONTENT_TYPE = "Content-Type";

    private static final Duration TIMEOUT = Duration.ofMinutes(1);

    private static HttpRequest.Builder withBody(Meta meta, HttpRequest.Builder requestBuilder, Object[] args) {
        if (meta.post()) {
            byte[] body = meta.body(args);
            return requestBuilder.POST(HttpRequest.BodyPublishers.ofByteArray(body, 0, body.length));
        }
        return requestBuilder;
    }
}
