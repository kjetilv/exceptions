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

package unearth.norest;

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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

class DefaultInvoker extends AbstractMetable<DefaultInvoker> implements InvocationHandler {
    
    private final Class<?> type;
    
    private final URI uri;
    
    DefaultInvoker(Class<?> type, URI uri, ObjectMapper objectMapper) {
        this(type, uri, objectMapper, null);
    }
    
    private DefaultInvoker(Class<?> type, URI uri, ObjectMapper objectMapper, List<Transformer<?>> transformers) {
        super(objectMapper, transformers);
        this.type = type;
        this.uri = Objects.requireNonNull(uri, "uri").toASCIIString().endsWith("/")
            ? uri
            : URI.create(uri.toASCIIString() + "/");
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
        return response(
            meta,
            request(uri, meta, args));
    }
    
    @Override
    protected DefaultInvoker with(List<Transformer<?>> list) {
        return new DefaultInvoker(type, uri, getObjectMapper(), list);
    }
    
    private Object response(Meta meta, HttpRequest request) {
        if (meta.isReturnData()) {
            HttpResponse<InputStream> response = response(request);
            if (response.statusCode() == 204 && meta.isReturnOptional()) {
                return Optional.empty();
            }
            Object object = parse(meta, response);
            return wrap(meta, object);
        }
        response(request);
        return null;
    }
    
    private Object parse(Meta meta, HttpResponse<InputStream> response) {
        try (InputStream body = response.body()) {
            return readBytes(meta.getReturnType(), body);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read response: " + response, e);
        }
    }
    
    private HttpRequest request(URI root, Meta meta, Object... args) {
        HttpRequest.Builder builder = base(root, meta, args);
        return meta.post()
            ? builder.POST(bodyPublisher(meta, args)).build()
            : builder.build();
    }
    
    private HttpRequest.BodyPublisher bodyPublisher(Meta meta, Object[] args) {
        return meta.bodyArgument(args)
            .map(body ->
                meta.isStringBody()
                    ? bytes(body.toString())
                    : writeBytes(body))
            .map(bytes ->
                HttpRequest.BodyPublishers.ofByteArray(bytes, 0, bytes.length))
            .orElseGet(
                HttpRequest.BodyPublishers::noBody);
    }
    
    private static final String CONTENT_TYPE = "Content-Type";
    
    private static final Duration TIMEOUT = Duration.ofMinutes(1);
    
    private static final int INTERNAL_ERROR = 500;
    
    private static final int REQUEST_ERROR = 400;
    
    private static final int REDIRECT = 300;
    
    private static byte[] bytes(Object string) {
        return string.toString().getBytes(StandardCharsets.UTF_8);
    }
    
    private static HttpRequest.Builder base(URI root, Meta meta, Object... args) {
        URI uri = URI.create(root.toASCIIString() + meta.path(args));
        return HttpRequest.newBuilder()
            .uri(uri)
            .header(CONTENT_TYPE, meta.contentType());
    }
    
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
    
    private static HttpClient newClient() {
        return HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }
    
    private static String possibly(String error) {
        return error == null || error.isBlank() ? "" : "\n" + error.trim();
    }
    
    private static String error(HttpResponse<InputStream> response) {
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))
        ) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "<Failed to read error response: " + e + ">";
        }
    }
}
