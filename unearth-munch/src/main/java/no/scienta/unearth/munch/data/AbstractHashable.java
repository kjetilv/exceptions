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

import no.scienta.unearth.munch.util.Memoizer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("WeakerAccess")
public abstract class AbstractHashable implements Hashable {

    private final Supplier<UUID> supplier = Hasher.uuid(this);

    private final Supplier<String> toString = Memoizer.get(() ->
        getClass().getSimpleName() + "[" + toStringIdentifier() + toStringContents() + "]");

    protected final void hashString(Consumer<byte[]> hash, String string) {
        hashStrings(hash, string);
    }

    protected final void hashStrings(Consumer<byte[]> hash, String... strings) {
        hashStrings(hash, Arrays.asList(strings));
    }

    protected final void hashStrings(Consumer<byte[]> hash, Collection<String> strings) {
        strings.stream()
            .filter(Objects::nonNull)
            .forEach(s ->
                hash.accept(s.getBytes(StandardCharsets.UTF_8)));
    }

    protected final void hashHashables(Consumer<byte[]> h, Hashable... hashables) {
        hashHashables(h, Arrays.asList(hashables));
    }

    protected final void hashHashables(Consumer<byte[]> h, Collection<? extends Hashable> hasheds) {
        hasheds.stream()
            .filter(Objects::nonNull)
            .forEach(hashable -> hashable.hashTo(h));
    }

    protected final void hashLongs(Consumer<byte[]> hash, long... values) {
        for (long value : values) {
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(value);
            hash.accept(buffer.array());
        }
    }

    protected Object toStringIdentifier() {
        return getHash();
    }

    protected String toStringBody() {
        return null;
    }

    private String toStringContents() {
        String body = toStringBody();
        return body == null || body.isBlank() ? "" : " " + body.trim();
    }

    @Override
    public final UUID getHash() {
        return supplier.get();
    }

    @Override
    public final int hashCode() {
        return getHash().hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        return obj == this || obj.getClass() == getClass()
            && ((AbstractHashable) obj).getHash().equals(getHash());
    }

    @Override
    public final String toString() {
        return toString.get();
    }
}
