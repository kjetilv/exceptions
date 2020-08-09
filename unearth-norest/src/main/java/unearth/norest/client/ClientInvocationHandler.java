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

package unearth.norest.client;

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
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import unearth.norest.common.IOHandler;
import unearth.norest.common.Request;

class ClientInvocationHandler implements InvocationHandler {
    
    private final Class<?> type;
    
    private final URI uri;
    
    private final IOHandler ioHandler;
    
    private final RemotableMethods callableMethods;
    
    ClientInvocationHandler(Class<?> type, URI uri, IOHandler ioHandler, RemotableMethods callableMethods) {
        this.type = Objects.requireNonNull(type, "type");
        this.uri = Objects.requireNonNull(uri, "uri").toASCIIString().endsWith("/")
            ? uri
            : URI.create(uri.toASCIIString() + "/");
        this.ioHandler = ioHandler;
        this.callableMethods = callableMethods;
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        if (method.getDeclaringClass() == type) {
            RemotableMethod meta = callableMethods.get(method);
            return response(
                meta,
                request(uri, meta, args));
        }
        if (method.getDeclaringClass() == Object.class) {
            try {
                return method.invoke(this, args);
            } catch (Exception e) {
                throw new IllegalStateException(
                    "Failed to invoke " + Object.class.getName() + "" + method + ": " + Arrays.toString(args), e);
            }
        }
        throw new IllegalArgumentException(
            "Unwilling to invoke " + method + " on " + proxy + ": " + Arrays.toString(args));
    }
    
    private Object response(RemotableMethod callableMethod, HttpRequest request) {
        if (callableMethod.isReturnData()) {
            HttpResponse<InputStream> response = response(request);
            if (response.statusCode() == 204 && callableMethod.isReturnOptional()) {
                return Optional.empty();
            }
            Object object = parse(callableMethod, response);
            return callableMethod.wrapResponse(object);
        }
        response(request);
        return null;
    }
    
    private Object parse(RemotableMethod callableMethod, HttpResponse<InputStream> response) {
        try (InputStream body = response.body()) {
            return ioHandler.readBytes(callableMethod.getReturnType(), body);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read response: " + response, e);
        }
    }
    
    private HttpRequest request(URI root, RemotableMethod callableMethod, Object... args) {
        HttpRequest.Builder builder = base(root, callableMethod, args);
        return callableMethod.getHttpMethod() == Request.Method.POST
            ? builder.POST(bodyPublisher(callableMethod, args)).build()
            : builder.build();
    }
    
    private HttpRequest.BodyPublisher bodyPublisher(RemotableMethod callableMethod, Object[] args) {
        return callableMethod.bodyArgument(args)
            .map(body ->
                callableMethod.isStringBody()
                    ? bytes(body.toString())
                    : ioHandler.writeBytes(body))
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
    
    private static HttpRequest.Builder base(URI root, RemotableMethod meta, Object... args) {
        URI uri = URI.create(root.toASCIIString() + meta.path(args));
        return HttpRequest.newBuilder()
            .uri(uri)
            .header(CONTENT_TYPE, meta.getContentType());
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
