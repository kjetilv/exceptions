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

    private final Map<Method, MethodMeta> analysisMap = new HashMap<>();

    Invoker(URI uri, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.uri = uri;
    }

    private MethodMeta handlerFor(Method method) {
        return analysisMap.computeIfAbsent(method, m ->
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
        MethodMeta methodHandler = handlerFor(method);
        URI uri = URI.create(this.uri.toASCIIString() + methodHandler.path(args));

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
        requestBuilder.uri(uri);
        if (methodHandler.post()) {
            byte[] body = methodHandler.body(args);
            requestBuilder.POST(HttpRequest.BodyPublishers.ofByteArray(body, 0, body.length));
        }

        HttpClient.Builder clientBuilder = HttpClient.newBuilder();
        clientBuilder.connectTimeout(Duration.ofMinutes(1));
        clientBuilder.followRedirects(HttpClient.Redirect.NEVER);
        clientBuilder.version(HttpClient.Version.HTTP_2);

        HttpRequest request = requestBuilder.build();
        HttpClient client = clientBuilder.build();

        HttpResponse<InputStream> response = getHttpResponse(request, client);
        return handle(methodHandler, response);
    }

    private Object handle(MethodMeta methodHandler, HttpResponse<InputStream> response) {
        if (response.statusCode() >= 500) {
            throw new IllegalStateException("Internal server error: " + response);
        }
        if (response.statusCode() >= 400) {
            throw new IllegalArgumentException("Invalid request: " + response);
        }
        if (response.statusCode() >= 300) {
            throw new IllegalArgumentException("Unsupported redirect: " + response);
        }
        return methodHandler.res(response.body());
    }

    private HttpResponse<InputStream> getHttpResponse(HttpRequest request, HttpClient client) {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send " + request, e);
        }
    }
}
