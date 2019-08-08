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

package no.scienta.unearth.client.main;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.scienta.unearth.client.UnearthlyClient;
import no.scienta.unearth.client.dto.FaultIdDto;
import no.scienta.unearth.client.dto.Submission;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

public class Feed {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .registerModule(new Jdk8Module())
        .registerModule(new JavaTimeModule());

    public static void main(String[] args) {
        URI uri = URI.create(args[0]);
        Collection<UUID> uuids =
            Arrays.stream(args).skip(1).map(UUID::fromString).collect(Collectors.toList());
        UnearthlyClient client = UnearthlyClient.connect(uri);

        uuids.stream()
            .map(Feed::faultId)
            .map(faultIdDto ->
                client.fault(faultIdDto, UnearthlyClient.StackType.FULL))
            .map(value -> {
                try {
                    return OBJECT_MAPPER.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                    throw new IllegalStateException(e);
                }
            })
            .forEach(System.out::println);

        String input = stdin();
        Submission submit = client.submit(input);
        System.out.println(submit.faultId);
        System.out.println(submit.faultStrandId);
        System.out.println(submit.faultEventId);
    }

    private static FaultIdDto faultId(UUID id) {
        return new FaultIdDto() {{
            uuid = id;
        }};
    }

    private static String stdin() {
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(System.in, StandardCharsets.UTF_8))
        ) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new IllegalStateException("Could not read", e);
        }
    }
}
