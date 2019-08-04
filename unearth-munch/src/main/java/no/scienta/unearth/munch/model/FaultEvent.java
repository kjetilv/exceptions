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

import no.scienta.unearth.munch.base.AbstractHashableIdentifiable;
import no.scienta.unearth.munch.id.FaultEventId;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class FaultEvent extends AbstractHashableIdentifiable<FaultEventId> {

    private final int throwableHashCode;

    private final Fault fault;

    private final LogEntry logEntry;

    private final EventSuppression suppression;

    private final long time;

    private final long globalSequenceNo;

    private final long faultStrandSequenceNo;

    private final long faultSequenceNo;

    public FaultEvent(
        int throwableHashCode,
        Fault fault,
        LogEntry logEntry,
        Instant time,
        EventSuppression suppression
    ) {
        this(throwableHashCode,
            fault,
            logEntry,
            Objects.requireNonNull(time, "time").toEpochMilli(),
            -1L,
            -1L,
            -1L,
            suppression);
    }

    private FaultEvent(
        int throwableHashCode,
        Fault fault,
        LogEntry logEntry,
        long time,
        long globalSequenceNo,
        long faultStrandSequenceNo,
        long faultSequenceNo,
        EventSuppression suppression
    ) {
        this.throwableHashCode = throwableHashCode;
        this.fault = Objects.requireNonNull(fault, "fault");
        this.logEntry = logEntry;
        this.time = time;
        this.globalSequenceNo = globalSequenceNo;
        this.faultStrandSequenceNo = faultStrandSequenceNo;
        this.faultSequenceNo = faultSequenceNo;
        this.suppression = suppression == null
            ? EventSuppression.UNKNOWN
            : suppression;
    }

    public int getThrowableHashCode() {
        return throwableHashCode;
    }

    public Fault getFault() {
        return fault;
    }

    public LogEntry getLogEntry() {
        return logEntry;
    }

    public Instant getTime() {
        return Instant.ofEpochMilli(time);
    }

    public Long getGlobalSequenceNo() {
        return globalSequenceNo;
    }

    public Long getFaultStrandSequenceNo() {
        return faultStrandSequenceNo;
    }

    public Long getFaultSequenceNo() {
        return faultSequenceNo;
    }

    public EventSuppression getSuppression() {
        return suppression;
    }

    public boolean isSequenced() {
        return globalSequenceNo >= 0;
    }

    public FaultEvent sequence(
        long globalSequenceNo,
        long faultStrandSequenceNo,
        long faultSequenceNo
    ) {
        return new FaultEvent(
            throwableHashCode,
            fault,
            logEntry,
            time,
            valid(globalSequenceNo),
            valid(faultStrandSequenceNo),
            valid(faultSequenceNo),
            suppression);
    }

    private static long valid(long seqNo) {
        if (seqNo < 0) {
            throw new IllegalArgumentException("Invalid seqNo: " + seqNo);
        }
        return seqNo;
    }

    @Override
    protected FaultEventId id(UUID hash) {
        return new FaultEventId(hash);
    }

    @Override
    protected String toStringBody() {
        String time = getTime().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        return fault.getId() +
            (logEntry == null ? "" : "/" + logEntry.getId()) +
            "@" + time +
            (suppression == EventSuppression.SUPPRESSED ? "<suppr> " : " ") +
            "g#" + globalSequenceNo + " ft#" + faultStrandSequenceNo + " f#" + faultSequenceNo;
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hash(h, time, faultSequenceNo, faultStrandSequenceNo, globalSequenceNo);
        hash(h, fault, logEntry);
    }
}
