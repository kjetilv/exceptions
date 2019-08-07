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
import no.scienta.unearth.client.dto.*;
import no.scienta.unearth.client.proto.Proto;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class DefaultUnearthlyClient implements UnearthlyClient {

    private final UnearthlyAPI unearthlyService;

    DefaultUnearthlyClient(URI uri) {
        this.unearthlyService = Proto.type(UnearthlyAPI.class, uri, OBJECT_MAPPER);
    }

    @Override
    public Throwable throwable(FaultIdDto faultId) {
        FaultDto body = unearthlyService.fault(faultId, true, false);
        if (body == null) {
            throw new IllegalArgumentException("No fault returned: " + faultId);
        }
        return toChameleon(body);
    }

    @Override
    public Submission submit(String throwable) {
        try {
            return unearthlyService.throwable(throwable);
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to submit " + (throwable.length() > 20 ? throwable.substring(0, 20) + " ..." : throwable), e);
        }
    }

    @Override
    public Submission submit(Throwable throwable) {
        try {
            return unearthlyService.throwable(toString(throwable));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to submit " + throwable, e);
        }
    }

    @Override
    public FaultDto fault(FaultIdDto faultId, StackType stackType) {
        return unearthlyService.fault(
            faultId,
            stackType == StackType.FULL,
            stackType == StackType.PRINT);
    }

    @Override
    public FaultStrandDto faultStrand(FaultStrandIdDto faultStrandId, StackType stackType) {
        return unearthlyService.faultStrand(
            faultStrandId,
            stackType == StackType.FULL,
            stackType == StackType.PRINT);
    }

    @Override
    public CauseDto cause(CauseIdDto causeId, StackType stackType) {
        return unearthlyService.cause(
            causeId,
            stackType == StackType.FULL,
            stackType == StackType.PRINT);
    }

    @Override
    public CauseStrandDto causeStrand(CauseStrandIdDto causeStrandId, StackType stackType) {
        return unearthlyService.causeStrand(
            causeStrandId,
            stackType == StackType.FULL,
            stackType == StackType.PRINT);
    }

    @Override
    public FaultEventDto faultEvent(FaultEventIdDto faultEventId) {
        return unearthlyService.faultEvent(faultEventId);
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        .registerModule(new Jdk8Module())
        .registerModule(new JavaTimeModule());

    private static Throwable toChameleon(FaultDto faultDto) {
        List<CauseDto> list =
            Arrays.stream(faultDto.causes).collect(Collectors.toCollection(ArrayList::new));
        Collections.reverse(list);
        return list.stream().reduce(
            null,
            DefaultUnearthlyClient::toChameleon,
            (t1, t2) -> {
                throw new IllegalStateException("No combine: " + t1 + " <> " + t2);
            });
    }

    private static String toString(Throwable throwable) {
        return new String(bytes(throwable), StandardCharsets.UTF_8);
    }

    private static byte[] bytes(Throwable throwable) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (PrintWriter pw = new PrintWriter(baos)) {
                throwable.printStackTrace(pw);
            }
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize: " + throwable, e);
        }
    }

    private static Throwable toChameleon(Throwable throwable, CauseDto cause) {
        Throwable exception =
            new ChameleonException(cause.causeStrand.className, cause.message, throwable);
        Optional.of(cause.causeStrand)
            .map(dto -> dto.fullStack)
            .map(DefaultUnearthlyClient::stackTrace)
            .ifPresent(exception::setStackTrace);
        return exception;
    }

    private static StackTraceElement[] stackTrace(StackTraceElementDto[] fullStack) {
        return Arrays.stream(fullStack)
            .map(frame ->
                new StackTraceElement(
                    frame.declaringClass,
                    frame.methodName,
                    frame.fileName,
                    frame.lineNumber == null
                        ? -1
                        : frame.lineNumber
                )).toArray(StackTraceElement[]::new);
    }
}
