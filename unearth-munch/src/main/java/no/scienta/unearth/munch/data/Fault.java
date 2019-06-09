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

import no.scienta.unearth.munch.ids.FaultId;
import no.scienta.unearth.munch.util.Streams;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Fault extends AbstractHashableIdentifiable<FaultId> {

    public static Fault create(Throwable throwable) {
        return new Fault(FaultType.create(throwable), causes(throwable));
    }

    private final FaultType faultType;

    private final List<Cause> causes;

    private Fault(FaultType faultType, Collection<Cause> causes) {
        this.faultType = Objects.requireNonNull(faultType);
        this.causes = causes == null || causes.isEmpty()
            ? Collections.emptyList()
            : List.copyOf(causes);
        if (this.faultType.getCauseCount() != this.causes.size()) {
            throw new IllegalStateException(
                "Expected same arity: " + this.faultType.getCauseTypes().size() + "/" + this.causes.size());
        }
    }

    public FaultType getFaultType() {
        return faultType;
    }

    public List<Cause> getCauses() {
        return causes;
    }

    public ThrowableDto toThrowableDto() {
        return Streams.reverse(causes)
            .reduce(null, (dto, cause) -> cause.toThrowableDto(dto), noCombine());
    }

    public Throwable toThrowable() {
        return Streams.reverse(causes)
            .reduce(null,
                (t, cause) ->
                    cause.toThrowable(t),
                noCombine());
    }

    private Stream<Integer> reversedRange(List<?> elements) {
        return IntStream.range(0, elements.size())
            .map(i -> elements.size() - i - 1)
            .boxed();
    }

    private static List<Cause> causes(Throwable throwable) {
        return Streams.causes(throwable).map(Cause::create).collect(Collectors.toList());
    }

    private static <T> BinaryOperator<T> noCombine() {
        return (t1, t2) -> {
            throw new IllegalStateException("No combine: " + t1 + " <> " + t2);
        };
    }

    @Override
    protected FaultId id(UUID hash) {
        return new FaultId(hash);
    }

    @Override
    protected String toStringBody() {
        return "faultType:" + faultType + ": " + causes.size();
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hashHashables(h, faultType);
        hashHashables(h, causes);
    }
}
