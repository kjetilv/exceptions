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

package unearth.client;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import unearth.api.UnearthlyAPI;
import unearth.api.dto.CauseDto;
import unearth.api.dto.CauseIdDto;
import unearth.api.dto.CauseStrandDto;
import unearth.api.dto.CauseStrandIdDto;
import unearth.api.dto.EventSequenceDto;
import unearth.api.dto.FaultDto;
import unearth.api.dto.FaultEventSequenceDto;
import unearth.api.dto.FaultIdDto;
import unearth.api.dto.FaultStrandDto;
import unearth.api.dto.FaultStrandEventSequenceDto;
import unearth.api.dto.FaultStrandIdDto;
import unearth.api.dto.FeedEntryDto;
import unearth.api.dto.FeedEntryIdDto;
import unearth.api.dto.StackTraceElementDto;
import unearth.api.dto.Submission;
import unearth.norest.Proto;

public class DefaultUnearthlyClient implements UnearthlyClient {

    private final UnearthlyAPI unearthlyService;

    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        .registerModule(new Jdk8Module())
        .registerModule(new JavaTimeModule());

    DefaultUnearthlyClient(URI uri) {
        this.unearthlyService = Proto.type(UnearthlyAPI.class, uri, OBJECT_MAPPER);
    }

    @Override
    public Submission submit(String throwable) {
        return unearthlyService.throwable(throwable);
    }

    @Override
    public Submission submit(Throwable throwable) {
        return unearthlyService.throwable(toString(throwable));
    }

    @Override
    public Optional<Throwable> throwable(FaultIdDto faultId) {
        Optional<FaultDto> fault = unearthlyService.fault(faultId, true, false);
        return fault.map(DefaultUnearthlyClient::toChameleon);
    }

    @Override
    public Optional<FaultDto> fault(FaultIdDto faultId, StackType stackType) {
        return unearthlyService.fault(
            faultId,
            stackType == StackType.FULL,
            stackType == StackType.PRINT);
    }

    @Override
    public Optional<FaultStrandDto> faultStrand(FaultStrandIdDto faultStrandId, StackType stackType) {
        return unearthlyService.faultStrand(
            faultStrandId,
            stackType == StackType.FULL,
            stackType == StackType.PRINT);
    }

    @Override
    public Optional<CauseDto> cause(CauseIdDto causeId, StackType stackType) {
        return unearthlyService.cause(
            causeId,
            stackType == StackType.FULL,
            stackType == StackType.PRINT);
    }

    @Override
    public Optional<CauseStrandDto> causeStrand(CauseStrandIdDto causeStrandId, StackType stackType) {
        return unearthlyService.causeStrand(
            causeStrandId,
            stackType == StackType.FULL,
            stackType == StackType.PRINT);
    }

    @Override
    public Optional<FeedEntryDto> feedEntry(FeedEntryIdDto faultEventId) {
        return unearthlyService.feedEntry(faultEventId);
    }

    @Override
    public long globalFeedMax() {
        return unearthlyService.globalFeedLimit();
    }

    @Override
    public long faultFeedMax(FaultIdDto faultId) {
        return unearthlyService.faultFeedLimit(faultId);
    }

    @Override
    public long faultStrandFeedMax(FaultStrandIdDto faultStrandId) {
        return unearthlyService.faultStrandFeedLimit(faultStrandId);
    }

    @Override
    public EventSequenceDto globalFeed(Page page, StackType stackType) {
        return unearthlyService.globalFeed(
            page.getPageNo() * page.getPageSize(),
            page.getPageNo(),
            stackType == StackType.FULL,
            stackType == StackType.PRINT);
    }

    @Override
    public FaultEventSequenceDto faultFeed(FaultIdDto faultId, Page page, StackType stackType) {
        return unearthlyService.faultFeed(
            faultId,
            page.getPageNo() * page.getPageSize(),
            page.getPageNo(),
            stackType == StackType.FULL,
            stackType == StackType.PRINT);
    }

    @Override
    public FaultStrandEventSequenceDto faultStrandFeed(FaultStrandIdDto faultStrandId, Page page, StackType stackType) {
        return unearthlyService.faultStrandFeed(
            faultStrandId,
            page.getPageNo() * page.getPageSize(),
            page.getPageNo(),
            stackType == StackType.FULL,
            stackType == StackType.PRINT);
    }

    private static Throwable toChameleon(FaultDto faultDto) {
        List<CauseDto> list = new ArrayList<>(faultDto.getCauses());
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
            new ChameleonException(cause.getCauseStrand().getClassName(), cause.getMessage(), throwable);
        Optional.of(cause.getCauseStrand())
            .map(CauseStrandDto::getFullStack)
            .map(DefaultUnearthlyClient::stackTrace)
            .ifPresent(exception::setStackTrace);
        return exception;
    }

    private static StackTraceElement[] stackTrace(Collection<StackTraceElementDto> fullStack) {
        return fullStack.stream()
            .map(frame ->
                new StackTraceElement(
                    frame.getDeclaringClass(),
                    frame.getMethodName(),
                    frame.getFileName(),
                    frame.getLineNumber()== null
                        ? -1
                        : frame.getLineNumber()
                )).toArray(StackTraceElement[]::new);
    }
}
