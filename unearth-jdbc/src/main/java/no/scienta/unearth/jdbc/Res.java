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

package no.scienta.unearth.jdbc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

interface Res {

    String getString();

    Boolean getBoolean();

    Integer getInt();

    Long getLong();

    UUID getUUID();

    Instant getInstant();

    boolean next();

    default <T> Stream<T> ifNext(Function<Res, T> apply) {
        return next() ? Optional.ofNullable(apply.apply(this)).stream(): Stream.empty();
    }

    default <T> Stream<T> ifNext(Supplier<T> apply) {
        return next() ? Optional.ofNullable(apply.get()).stream() : Stream.empty();
    }

    default <T> Optional<T> ifNextOne(Function<Res, T> apply) {
        return next() ? Optional.ofNullable(apply.apply(this)): Optional.empty();
    }

    default <T> Optional<T> ifNextOne(Supplier<T> apply) {
        return next() ? Optional.ofNullable(apply.get()) : Optional.empty();
    }
}
