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

import no.scienta.unearth.munch.base.AbstractHashable;

import java.util.Objects;
import java.util.function.Consumer;

public class FaultEvents extends AbstractHashable {

    private final FaultEvent event;

    private final FaultEvent previous;

    public FaultEvents(FaultEvent event, FaultEvent previous) {
        this.event = Objects.requireNonNull(event, "event");
        this.previous = previous;
    }

    public FaultEvent getEvent() {
        return event;
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hash(h, event, previous);
    }

    @Override
    protected String toStringBody() {
        return event.getHash() + "[" + event.getFaultId() + "@" + event.getTime()
            + (previous == null ? "" : " previous @" + previous.getTime())
            + "]";
    }
}
