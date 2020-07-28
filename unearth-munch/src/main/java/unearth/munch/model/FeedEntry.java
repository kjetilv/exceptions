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

import java.util.UUID;
import java.util.function.Consumer;

import unearth.munch.id.AbstractHashableIdentifiable;
import unearth.munch.id.FeedEntryId;

@SuppressWarnings("unused")
public final class FeedEntry extends AbstractHashableIdentifiable<FeedEntryId> {

    private final FaultEvent faultEvent;

    private final Long globalSequenceNo;

    private final Long faultStrandSequenceNo;

    private final Long faultSequenceNo;

    public FeedEntry(
        FaultEvent faultEvent,
        Long globalSequenceNo,
        Long faultStrandSequenceNo,
        Long faultSequenceNo
    ) {
        this.faultEvent = faultEvent;
        this.globalSequenceNo = globalSequenceNo;
        this.faultStrandSequenceNo = faultStrandSequenceNo;
        this.faultSequenceNo = faultSequenceNo;
    }

    public FaultEvent getFaultEvent() {
        return faultEvent;
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

    public boolean isSequenced() {
        return globalSequenceNo >= 0;
    }

    public FeedEntry sequence(
        long globalSequenceNo,
        long faultStrandSequenceNo,
        long faultSequenceNo
    ) {
        return new FeedEntry(
            faultEvent,
            valid(globalSequenceNo),
            valid(faultStrandSequenceNo),
            valid(faultSequenceNo)
        );
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hash(h, faultSequenceNo, faultStrandSequenceNo, globalSequenceNo);
        hash(h, faultEvent);
    }

    @Override
    protected FeedEntryId id(UUID hash) {
        return new FeedEntryId(hash);
    }

    @Override
    protected String toStringBody() {
        return faultEvent.toStringBody() +
            " g#" + globalSequenceNo + " ft#" + faultStrandSequenceNo + " f#" + faultSequenceNo;
    }

    private static Long valid(long seqNo) {
        if (seqNo < 0) {
            throw new IllegalArgumentException("Invalid seqNo: " + seqNo);
        }
        return seqNo;
    }
}
