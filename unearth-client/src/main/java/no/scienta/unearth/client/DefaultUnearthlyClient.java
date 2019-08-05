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

package no.scienta.unearth.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import no.scienta.unearth.dto.*;
import no.scienta.unearth.munch.parser.ThrowableParser;
import no.scienta.unearth.util.Throwables;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class DefaultUnearthlyClient implements UnearthlyClient {

    private final UnearthlyAPI unearthlyService;

    DefaultUnearthlyClient(URI uri) {
        Retrofit.Builder builder = new Retrofit.Builder().baseUrl(uri.toASCIIString())
            .addConverterFactory(new ThrowablesConverterFactory())
            .addConverterFactory(new IdConverterFactory())
            .addConverterFactory(JacksonConverterFactory.create(OBJECT_MAPPER));
        this.unearthlyService = builder.build().create(UnearthlyAPI.class);
    }

    @Override
    public Submission submit(Path path) {
        try {
            return submit(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read " + path, e);
        }
    }

    @Override
    public Throwable throwable(FaultIdDto faultId) {
        try {
            return unearthlyService.throwable(faultId.getUuid()).execute().body();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to retrieve " + faultId, e);
        }
    }

    @Override
    public Submission submit(String string) {
        try {
             return get(unearthlyService.throwable(
                 RequestBody.create(
                     MediaType.get("text/plain;charset=UTF-8"),
                     string.getBytes(StandardCharsets.UTF_8))));
         } catch (Exception e) {
             throw new IllegalStateException(
                 "Failed to submit " + (string.length() > 20 ? string.substring(0, 20) + " ..." : string), e);
         }
    }

    @Override
    public Submission submit(Throwable t) {
        try {
            return get(unearthlyService.throwable(t));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to submit " + t, e);
        }
    }

    @Override
    public FaultDto fault(FaultIdDto faultId, StackType stackType) {
        return get(unearthlyService.fault(
            faultId,
            stackType == StackType.FULL,
            stackType == StackType.PRINT
        ));
    }

    @Override
    public FaultStrandDto faultStrand(FaultStrandIdDto faultStrandId, StackType stackType) {
        return get(unearthlyService.faultStrand(
            faultStrandId,
            stackType == StackType.FULL,
            stackType == StackType.PRINT));
    }

    @Override
    public CauseDto cause(CauseIdDto causeId, StackType stackType) {
        return get(unearthlyService.cause(
            causeId,
            stackType == StackType.FULL,
            stackType == StackType.PRINT
        ));
    }

    @Override
    public CauseStrandDto causeStrand(CauseStrandIdDto causeStrandId, StackType stackType) {
        return get(unearthlyService.causeStrand(
            causeStrandId,
            stackType == StackType.FULL,
            stackType == StackType.PRINT
        ));
    }

    @Override
    public FaultEventDto faultEvent(FaultEventIdDto faultEventId) {
        return get(unearthlyService.faultEvent(faultEventId));
    }

    private <T> T get(Call<T> call) {
        Response<T> response = response(call);
        if (response.code() < 300) {
            return response.body();
        }
        throw new IllegalStateException(
            "Unexpected response: " + response.code() + ": " + response.message() + errorBody(response));
    }

    private static <T> Response<T> response(Call<T> call) {
        try {
            return call.execute();
        } catch (IOException e) {
            throw new IllegalStateException("Execute call: " + call, e);
        }
    }

    @NotNull
    private <T> String errorBody(Response<T> response) {
        return Optional.ofNullable(response.errorBody())
            .map(body -> {
                try {
                    return body.string();
                } catch (IOException e) {
                    return "<body fail: " + e + ">";
                }
            })
            .filter(body ->
                !body.trim().isEmpty())
            .map(String::trim)
            .map(body ->
                "\n  " + body)
            .orElse("");
    }

    private static class IdConverterFactory extends Converter.Factory {
        @Override
        public Converter<?, String> stringConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
            if (type == FaultIdDto.class) {
                return (Converter<FaultIdDto, String>) value -> value.getUuid().toString();
            }
            if (type == FaultStrandIdDto.class) {
                return (Converter<FaultStrandIdDto, String>) value -> value.getUuid().toString();
            }
            if (type == CauseIdDto.class) {
                return (Converter<CauseIdDto, String>) value -> value.getUuid().toString();
            }
            if (type == CauseStrandIdDto.class) {
                return (Converter<CauseStrandIdDto, String>) value -> value.getUuid().toString();
            }
            if (type == FaultEventIdDto.class) {
                return (Converter<FaultEventIdDto, String>) value -> value.getUuid().toString();
            }
            if (type == IdDto.class) {
                return (Converter<IdDto, String>) value -> value.getUuid().toString();
            }
            return null;
        }
    }

    private static class ThrowablesConverterFactory extends Converter.Factory {

        @Override
        public Converter<ResponseBody, ?> responseBodyConverter(
            Type type,
            Annotation[] annotations,
            Retrofit retrofit
        ) {
            if (type instanceof Class<?> && Throwable.class.isAssignableFrom((Class<?>) type)) {
                return responseBody ->
                    ThrowableParser.parse(responseBody.string());
            }
            return null;
        }

        @Override
        public Converter<Throwable, RequestBody> requestBodyConverter(
            Type type,
            Annotation[] parameterAnnotations,
            Annotation[] methodAnnotations,
            Retrofit retrofit
        ) {
            if (type == Throwable.class) {
                return (Throwable t) ->
                    RequestBody.create(MediaType.get("text/plain"), Throwables.string(t));
            }
            return null;
        }
    }

    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        .registerModule(new Jdk8Module())
        .registerModule(new JavaTimeModule())
        .registerModule(new KotlinModule());
}
