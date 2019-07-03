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
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import no.scienta.unearth.dto.Submission;
import no.scienta.unearth.munch.id.FaultId;
import no.scienta.unearth.munch.json.IdModule;
import no.scienta.unearth.munch.parser.ThrowableParser;
import no.scienta.unearth.munch.util.Throwables;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@SuppressWarnings("WeakerAccess")
public class UnearthlyClient implements UnearthlyService {

    private final UnearthlyService unearthlyService;

    private final ObjectMapper objectMapper;

    public static UnearthlyClient connect(URI uri) {
        return new UnearthlyClient(uri);
    }

    public UnearthlyClient(URI uri) {
        this.objectMapper =
            new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .registerModule(new Jdk8Module())
                .registerModule(new IdModule().addDefaults())
                .registerModule(new KotlinModule());
        Converter.Factory jackson =
            JacksonConverterFactory.create(objectMapper);
        Converter.Factory throwables = new Converter.Factory() {

            @Override
            public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
                return responseBody ->
                    ThrowableParser.parse(responseBody.string());
            }

            @Override
            public Converter<Throwable, RequestBody> requestBodyConverter(
                Type type,
                Annotation[] parameterAnnotations,
                Annotation[] methodAnnotations,
                Retrofit retrofit
            ) {
                return (Throwable t) ->
                    RequestBody.create(MediaType.get("text/plain"), Throwables.string(t));
            }
        };
        this.unearthlyService =
            new Retrofit.Builder()
                .baseUrl(uri.toASCIIString())
                .addConverterFactory(jackson)
                .addConverterFactory(throwables)
                .build().create(UnearthlyService.class);
    }

    public Submission submit(Path path) {
        try {
            return submit(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read " + path, e);
        }
    }

    public Throwable retrieve(FaultId faultId) {
        try {
            return throwable(faultId.getHash()).execute().body();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to retrieve " + faultId, e);
        }
    }

    public Submission submit(String string) {
        return submit(ThrowableParser.parse(string));
    }

    public Submission submit(Throwable t) {
        try {
            return throwable(t).execute().body();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to submit: " + t, e);
        }
    }

    public String print(Submission submit) {
        try {
            return objectMapper.writeValueAsString(submit);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to print " + submit, e);
        }
    }

    @Override
    public Call<Submission> throwable(Throwable throwable) {
        return unearthlyService.throwable(throwable);
    }

    @Override
    public Call<Throwable> throwable(UUID uuid) {
        return unearthlyService.throwable(uuid);
    }
}
