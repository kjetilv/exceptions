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

package no.scienta.unearth.munch.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import no.scienta.unearth.munch.id.AbstractHashableIdentifiable;
import no.scienta.unearth.munch.id.FaultId;
import no.scienta.unearth.munch.id.FaultStrandId;
import no.scienta.unearth.munch.id.FeedEntryId;

@SuppressWarnings("unused")
public final class FaultEvent extends AbstractHashableIdentifiable<FeedEntryId> {

    private final Integer throwableHashCode;

    private final FaultId faultId;

    private final FaultStrandId faultStrandId;

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
            time
        );
    }

    public FaultEvent(
        FaultId faultId,
        FaultStrandId faultStrandId,
        Instant time
    ) {
        this(null, faultId, faultStrandId, time);
    }

    private FaultEvent(
        Integer throwableHashCode,
        FaultId faultId,
        FaultStrandId faultStrandId,
        Instant time
    ) {
        this.throwableHashCode = throwableHashCode;
        this.faultId = faultId;
        this.faultStrandId = faultStrandId;
        this.time = Objects.requireNonNull(time, "time");
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

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hash(h, time.toEpochMilli());
        hash(h, faultId);
    }

    @Override
    protected String toStringBody() {
        return "F: " + getFaultId() +
            "@" + getTime().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
    }

    @Override
    protected FeedEntryId id(UUID hash) {
        return new FeedEntryId(hash);
    }
}
