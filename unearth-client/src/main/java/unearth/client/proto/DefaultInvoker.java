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

package unearth.client.proto;

import java.io.BufferedReader;
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

import com.fasterxml.jackson.databind.ObjectMapper;

class DefaultInvoker implements InvocationHandler {
    
    private final Class<?> type;
    
    private final URI uri;
    
    private final ObjectMapper objectMapper;
    
    private final Map<Method, Meta> metas = new HashMap<>();
    
    private final Function<Object, byte[]> writeBytes = this::writeBytes;
    
    private final BiFunction<Class<?>, InputStream, Object> readBytes = this::readBytes;
    
    DefaultInvoker(Class<?> type, URI uri, ObjectMapper objectMapper) {
        this.type = type;
        this.uri = Objects.requireNonNull(uri, "uri").toASCIIString().endsWith("/")
            ? uri
            : URI.create(uri.toASCIIString() + "/");
        this.objectMapper = objectMapper;
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        if (method.getDeclaringClass() != type) {
            try {
                return method.invoke(this, args);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to proxy for " + method, e);
            }
        }
        Meta meta = meta(method);
        HttpRequest request = request(meta, args);
        if (meta.returnsData()) {
            HttpResponse<InputStream> httpResponse = response(request);
            if (httpResponse.statusCode() == 204 && meta.optional()) {
                return Optional.empty();
            }
            Object response = readResponse(meta, httpResponse);
            return meta.wrap(response);
        }
        response(request);
        return null;
    }
    
    private Meta meta(Method method) {
        return metas.computeIfAbsent(
            method,
            __ ->
                new DefaultMeta(method, writeBytes, readBytes));
    }
    
    private HttpRequest request(Meta meta, Object... args) {
        if (meta.post()) {
            byte[] body = meta.body(args);
            return base(meta, args)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body, 0, body.length))
                .build();
        }
        return base(meta, args).build();
    }
    
    private HttpRequest.Builder base(Meta meta, Object... args) {
        URI uri = URI.create(this.uri.toASCIIString() + meta.path(args));
        return HttpRequest.newBuilder().uri(uri)
            .header(CONTENT_TYPE, meta.contentType());
    }
    
    private byte[] writeBytes(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write body: " + value, e);
        }
    }
    
    private Object readBytes(Class<?> type, InputStream inputStream) {
        try {
            return objectMapper.readerFor(type).readValue(inputStream);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read response: " + type + " <= " + inputStream, e);
        }
    }
    
    private static final String CONTENT_TYPE = "Content-Type";
    
    private static final Duration TIMEOUT = Duration.ofMinutes(1);
    
    private static final int INTERNAL_ERROR = 500;
    
    private static final int REQUEST_ERROR = 400;
    
    private static final int REDIRECT = 300;
    
    private static HttpResponse<InputStream> response(HttpRequest request) {
        HttpResponse<InputStream> response = send(request);
        if (response.statusCode() >= INTERNAL_ERROR) {
            throw new IllegalStateException(
                "Internal server error: " + response + possibly(error(response)));
        }
        if (response.statusCode() >= REQUEST_ERROR) {
            throw new IllegalArgumentException(
                "Invalid request: " + response + possibly(error(response)));
        }
        if (response.statusCode() >= REDIRECT) {
            throw new IllegalArgumentException(
                "Unsupported redirect: " + response + possibly(error(response)));
        }
        return response;
    }
    
    private static HttpResponse<InputStream> send(HttpRequest request) {
        try {
            return newClient().send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send " + request, e);
        }
    }
    
    private static Object readResponse(Meta meta, HttpResponse<InputStream> response) {
        try (InputStream body = response.body()) {
            return meta.response(body);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read response: " + response, e);
        }
    }
    
    private static HttpClient newClient() {
        return HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }
    
    private static String possibly(String error) {
        return error == null || error.isBlank() ? "" : "\n" + error.trim();
    }
    
    private static String error(HttpResponse<InputStream> response) {
        try
            (
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                    response.body(),
                    StandardCharsets.UTF_8))
            ) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "<Failed to read error response: " + e + ">";
        }
    }
}
