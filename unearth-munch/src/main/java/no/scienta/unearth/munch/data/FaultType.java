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

import no.scienta.unearth.munch.base.AbstractHashableIdentifiable;
import no.scienta.unearth.munch.id.FaultTypeId;
import no.scienta.unearth.munch.util.Streams;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A fault type consists of a list of {@link CauseType cause types}.
 */
public class FaultType extends AbstractHashableIdentifiable<FaultTypeId> {

    private final List<CauseType> causeTypes;

    public static FaultType create(Throwable throwable) {
        List<Cause> causes = Streams.reverse(
            Streams.causes(throwable).map(Cause::create)
        ).collect(Collectors.toList());
        List<CauseType> causeTypes = causes.stream().map(Cause::getCauseType).collect(Collectors.toList());
        return new FaultType(causeTypes);
    }

    public List<CauseType> getCauseTypes() {
        return causeTypes;
    }

    public FaultType withCauseTypes(List<CauseType> causeTypes) {
        return new FaultType(causeTypes);
    }

    int getCauseCount() {
        return causeTypes.size();
    }

    private FaultType(Collection<CauseType> causeTypes) {
        if (Objects.requireNonNull(causeTypes).isEmpty()) {
            throw new IllegalArgumentException("Expected one or more causes");
        }
        this.causeTypes = List.copyOf(causeTypes);
    }

    @Override
    protected String toStringBody() {
        return "(" +
            causeTypes.stream().map(Objects::toString).collect(Collectors.joining(" <= ")) +
            ")";
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hashHashables(h, causeTypes);
    }

    @Override
    protected FaultTypeId id(UUID hash) {
        return new FaultTypeId(hash);
    }
}
