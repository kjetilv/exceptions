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

package no.scienta.unearth.munch.data;

import no.scienta.unearth.munch.ids.FaultEventId;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class FaultEvent extends AbstractHashableIdentifiable<FaultEventId> {

    private final Fault fault;

    private final long time;

    private final long globalSequence;

    private final long faultTypeSequence;

    private final long faultSequence;

    FaultEvent(Fault fault) {
        this(fault, null, null, null, null);
    }

    public FaultEvent(
        Fault fault,
        Instant time,
        Long globalSequence,
        Long faultTypeSequence,
        Long faultSequence
    ) {
        this.fault = Objects.requireNonNull(fault);
        this.time = Objects.requireNonNull(time).toEpochMilli();
        this.globalSequence = Objects.requireNonNull(globalSequence);
        this.faultTypeSequence = Objects.requireNonNull(faultTypeSequence);
        this.faultSequence = Objects.requireNonNull(faultSequence);
    }

    public Fault getFault() {
        return fault;
    }

    public Instant getTime() {
        return Instant.ofEpochMilli(time);
    }

    public Long getGlobalSequence() {
        return globalSequence;
    }

    public Long getFaultTypeSequence() {
        return faultTypeSequence;
    }

    public Long getFaultSequence() {
        return faultSequence;
    }

    @Override
    protected FaultEventId id(UUID hash) {
        return new FaultEventId(hash);
    }

    @Override
    protected String toStringBody() {
        String time = getTime().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        return fault.getId() + "@" + time +
            " g#" + globalSequence + " ft#" + faultTypeSequence + " f#" + faultSequence;
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hashHashables(h, fault);
        hashLongs(h, time, faultSequence, faultTypeSequence, globalSequence);
    }
}

