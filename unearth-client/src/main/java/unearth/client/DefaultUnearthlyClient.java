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
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import unearth.api.UnearthlyApi;
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
import unearth.norest.HandlerIO;
import unearth.norest.Transformer;
import unearth.norest.Transformers;
import unearth.norest.client.Proto;
import unearth.norest.common.JacksonIOHandler;
import unearth.norest.common.StringIOHandler;

import static unearth.norest.IO.ContentType.APPLICATION_JSON;
import static unearth.norest.IO.ContentType.TEXT_PLAIN;

public class DefaultUnearthlyClient implements UnearthlyClient {

    private final UnearthlyApi api;

    DefaultUnearthlyClient(URI uri) {
        this.api = Proto.type(
            UnearthlyApi.class,
            uri,
            new HandlerIO(Map.of(
                APPLICATION_JSON, JacksonIOHandler.withDefaults(new ObjectMapper()),
                TEXT_PLAIN, new StringIOHandler(StandardCharsets.UTF_8))),
            new Transformers(
                List.of(
                    Transformer.from(FaultIdDto.class, FaultIdDto::new),
                    Transformer.from(FaultStrandIdDto.class, FaultStrandIdDto::new),
                    Transformer.from(CauseIdDto.class, CauseIdDto::new),
                    Transformer.from(CauseStrandIdDto.class, CauseStrandIdDto::new),
                    Transformer.from(FeedEntryIdDto.class, FeedEntryIdDto::new))));
    }

    @Override
    public void quickPing() {
        api.pingHead();
    }

    @Override
    public boolean ping() {
        return api.ping().equalsIgnoreCase("pong");
    }

    @Override
    public Submission submit(String throwable) {
        return api.throwable(throwable);
    }

    @Override
    public Submission submit(Throwable throwable) {
        return api.throwable(toString(throwable));
    }

    @Override
    public Optional<Throwable> throwable(FaultIdDto faultId) {
        Optional<FaultDto> fault = api.fault(faultId, true, false);
        return fault.map(DefaultUnearthlyClient::toChameleon);
    }

    @Override
    public Optional<FaultDto> fault(FaultIdDto faultId, StackType stackType) {
        return api.fault(
            faultId,
            stackType == StackType.FULL,
            stackType == StackType.PRINT);
    }

    @Override
    public Optional<FaultStrandDto> faultStrand(FaultStrandIdDto faultStrandId, StackType stackType) {
        return api.faultStrand(
            faultStrandId,
            stackType == StackType.FULL,
            stackType == StackType.PRINT);
    }

    @Override
    public Optional<CauseDto> cause(CauseIdDto causeId, StackType stackType) {
        return api.cause(
            causeId,
            stackType == StackType.FULL,
            stackType == StackType.PRINT);
    }

    @Override
    public Optional<CauseStrandDto> causeStrand(CauseStrandIdDto causeStrandId, StackType stackType) {
        return api.causeStrand(
            causeStrandId,
            stackType == StackType.FULL,
            stackType == StackType.PRINT);
    }

    @Override
    public Optional<FeedEntryDto> feedEntry(FeedEntryIdDto faultEventId, StackType stackType) {
        return api.feedEntry(
            faultEventId,
            stackType == StackType.FULL,
            stackType == StackType.PRINT);
    }

    @Override
    public long globalFeedMax() {
        return api.globalFeedLimit();
    }

    @Override
    public long faultFeedMax(FaultIdDto faultId) {
        return api.faultFeedLimit(faultId);
    }

    @Override
    public long faultStrandFeedMax(FaultStrandIdDto faultStrandId) {
        return api.faultStrandFeedLimit(faultStrandId);
    }

    @Override
    public EventSequenceDto globalFeed(Page page, StackType stackType) {
        return api.globalFeed(
            (long) page.getPageNo() * page.getPageSize(),
            page.getPageNo(),
            stackType == StackType.FULL,
            stackType == StackType.PRINT);
    }

    @Override
    public FaultEventSequenceDto faultFeed(FaultIdDto faultId, Page page, StackType stackType) {
        return api.faultFeed(
            faultId,
            (long) page.getPageNo() * page.getPageSize(),
            page.getPageNo(),
            stackType == StackType.FULL,
            stackType == StackType.PRINT);
    }

    @Override
    public FaultStrandEventSequenceDto faultStrandFeed(
        FaultStrandIdDto faultStrandId,
        Page page,
        StackType stackType
    ) {
        return api.faultStrandFeed(
            faultStrandId,
            (long) page.getPageNo() * page.getPageSize(),
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
                    frame.getLineNumber() == null
                        ? -1
                        : frame.getLineNumber()
                )).toArray(StackTraceElement[]::new);
    }
}
