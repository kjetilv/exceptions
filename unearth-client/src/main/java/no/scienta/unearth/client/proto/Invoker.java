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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

class Invoker implements InvocationHandler {

    private final URI uri;

    private final ObjectMapper objectMapper;

    private final Map<Method, MethodMeta> meta = new HashMap<>();

    Invoker(URI uri, ObjectMapper objectMapper) {
        this.uri = uri;
        this.objectMapper = objectMapper;
        if (!this.uri.toASCIIString().endsWith("/")) {
            throw new IllegalArgumentException("Expected slashed URI: " + uri);
        }
    }

    private MethodMeta meta(Method method) {
        return meta.computeIfAbsent(method, m ->
            new MethodMeta(m, this::writeBytes, this::readBytes));
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
            throw new IllegalStateException("Failed to read response: " + type, e);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        MethodMeta meta = meta(method);
        URI uri = URI.create(this.uri.toASCIIString() + meta.path(args));
        HttpRequest request = newrRequest(meta, uri, args);
        HttpResponse<InputStream> response = getHttpResponse(request);
        failOnError(response);
        return responseBody(meta, response);
    }

    private HttpRequest newrRequest(MethodMeta meta, URI uri, Object[] args) {
        return withBody(meta, HttpRequest.newBuilder().uri(uri), args)
                .header("Content-Type", meta.contentType())
                .build();
    }

    private HttpClient newClient() {
        return HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    private Object responseBody(MethodMeta meta, HttpResponse<InputStream> response) {
        try (InputStream body = response.body()) {
            return meta.response(body);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read response: " + response, e);
        }
    }

    private void failOnError(HttpResponse<InputStream> response) {
        int result = response.statusCode();
        if (result >= 500) {
            throw new IllegalStateException("Internal server error: " + response);
        }
        if (result >= 400) {
            throw new IllegalArgumentException("Invalid request: " + response);
        }
        if (result >= 300) {
            throw new IllegalArgumentException("Unsupported redirect: " + response);
        }
    }

    private HttpResponse<InputStream> getHttpResponse(HttpRequest request) {
        try {
            return newClient().send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send " + request, e);
        }
    }

    private static final Duration TIMEOUT = Duration.ofMinutes(1);

    private static HttpRequest.Builder withBody(MethodMeta meta, HttpRequest.Builder requestBuilder, Object[] args) {
        if (meta.post()) {
            byte[] body = meta.body(args);
            return requestBuilder.POST(HttpRequest.BodyPublishers.ofByteArray(body, 0, body.length));
        }
        return requestBuilder;
    }
}
