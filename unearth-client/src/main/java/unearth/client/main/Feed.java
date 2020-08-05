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

package unearth.client.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import unearth.api.dto.CauseIdDto;
import unearth.api.dto.CauseStrandIdDto;
import unearth.api.dto.FaultIdDto;
import unearth.api.dto.FaultStrandIdDto;
import unearth.api.dto.FeedEntryIdDto;
import unearth.api.dto.Submission;
import unearth.client.UnearthlyClient;

public final class Feed {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .registerModule(new Jdk8Module())
        .registerModule(new JavaTimeModule());

    public static void main(String[] args) {
        URI uri = URI.create(args[0]);
        Collection<?> uuids =
            Arrays.stream(args).skip(1).map(Feed::fromString).collect(Collectors.toList());
        UnearthlyClient client = UnearthlyClient.connect(uri);

        uuids.stream()
            .map(fetchFrom(client))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(Feed::serialize)
            .forEach(System.out::println);

        String input = stdin();
        Submission submit = client.submit(input);
        System.out.println(submit.getFaultId());
        System.out.println(submit.getFaultStrandId());
        System.out.println(submit.getFeedEntryId());
    }

    private static String serialize(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Function<Object, Optional<?>> fetchFrom(UnearthlyClient client) {
        return id -> {
            if (id instanceof FaultIdDto) {
                return client.fault((FaultIdDto) id, UnearthlyClient.StackType.FULL);
            }
            if (id instanceof FaultStrandIdDto) {
                return client.faultStrand((FaultStrandIdDto) id, UnearthlyClient.StackType.FULL);
            }
            if (id instanceof CauseIdDto) {
                return client.cause((CauseIdDto) id, UnearthlyClient.StackType.FULL);
            }
            if (id instanceof CauseStrandIdDto) {
                return client.causeStrand((CauseStrandIdDto) id, UnearthlyClient.StackType.FULL);
            }
            if (id instanceof FeedEntryIdDto) {
                return client.feedEntry((FeedEntryIdDto) id);
            }
            throw new IllegalArgumentException("Invalid id: " + id);
        };
    }

    private static Object fromString(String input) {
        int split = input.indexOf(":");
        if (split == -1) {
            throw new IllegalStateException("Invalid reference: " + input);
        }
        UUID uuid = UUID.fromString(input.substring(split + 1));
        String type = input.substring(0, split).toLowerCase(Locale.ROOT);
        return switch (type) {
            case "fault" -> new FaultIdDto(uuid);
            case "fault-strand" -> new FaultStrandIdDto(uuid);
            case "cause" -> new CauseIdDto(uuid);
            case "cause-strand" -> new CauseStrandIdDto(uuid);
            case "feed-entry" -> new FeedEntryIdDto(uuid);
            default -> throw new IllegalStateException("Unused type " + type + ":" + uuid);
        };
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
