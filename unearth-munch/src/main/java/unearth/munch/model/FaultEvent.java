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

package unearth.munch.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import unearth.munch.id.AbstractHashableIdentifiable;
import unearth.munch.id.FaultId;
import unearth.munch.id.FaultStrandId;
import unearth.munch.id.FeedEntryId;

public final class FaultEvent extends AbstractHashableIdentifiable<FeedEntryId> {

    private final Integer throwableHashCode;

    private final FaultId faultId;

    private final FaultStrandId faultStrandId;

    private final LogEntry logEntry;

    private final Instant time;

    public FaultEvent(
        Integer throwableHashCode,
        Fault fault,
        LogEntry logEntry,
        Instant time
    ) {
        this(
            throwableHashCode,
            fault.getId(),
            fault.getFaultStrand().getId(),
            logEntry,
            time);
    }

    public FaultEvent(
        FaultId faultId,
        FaultStrandId faultStrandId,
        Instant time
    ) {
        this(null, faultId, faultStrandId, null, time);
    }

    private FaultEvent(
        Integer throwableHashCode,
        FaultId faultId,
        FaultStrandId faultStrandId,
        LogEntry logEntry,
        Instant time
    ) {
        this.throwableHashCode = throwableHashCode;
        this.faultId = faultId;
        this.faultStrandId = faultStrandId;
        this.logEntry = logEntry;
        this.time = Objects.requireNonNull(time, "time");
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hash(h, time.toEpochMilli());
        hash(h, faultId);
    }

    @Override
    protected StringBuilder withStringBody(StringBuilder sb) {
        return sb.append("F: ")
            .append(getFaultId())
            .append("@")
            .append(getTime().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
    }

    @Override
    protected FeedEntryId id(UUID hash) {
        return new FeedEntryId(hash);
    }

    public int getThrowableHashCode() {
        return throwableHashCode;
    }

    public FaultId getFaultId() {
        return faultId;
    }

    public FaultStrandId getFaultStrandId() {
        return faultStrandId;
    }

    public Instant getTime() {
        return time;
    }

    public LogEntry getLogEntry() {
        return logEntry;
    }
}
