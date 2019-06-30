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

package no.scienta.unearth.munch.base;

import no.scienta.unearth.munch.util.Memoizer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class AbstractHashable implements Hashable {

    /**
     * A supplier which computes {@link Hashable this hashable's} uuid with a {@link Memoizer}.
     */
    private final Supplier<UUID> supplier = Memoizer.get(uuid(this));

    private final Supplier<String> toString = Memoizer.get(() ->
        getClass().getSimpleName() + "[" + toStringIdentifier() + toStringContents() + "]");

    /**
     * Takes a {@link Hashable hashable} and returns a supplier which computs its UUID
     *
     * @param hashable Hashable
     * @return UUID supplier
     */
    private static Supplier<UUID> uuid(Hashable hashable) {
        return Memoizer.get(() -> {
            MessageDigest md5 = md5();
            hashable.hashTo(md5::update);
            return UUID.nameUUIDFromBytes(md5.digest());
        });
    }

    private static MessageDigest md5() {
        try {
            return MessageDigest.getInstance(HASH);
        } catch (Exception e) {
            throw new IllegalStateException("Expected " + HASH + " implementation", e);
        }
    }

    protected final void hash(Consumer<byte[]> hash, String string) {
        this.hashStrings(hash, Collections.singleton(string));
    }

    protected final void hash(Consumer<byte[]> hash, String... strings) {
        hashStrings(hash, Arrays.asList(strings));
    }

    private void hashStrings(Consumer<byte[]> hash, Collection<String> strings) {
        strings.stream()
            .filter(Objects::nonNull)
            .forEach(s ->
                hash.accept(s.getBytes(StandardCharsets.UTF_8)));
    }

    protected final void hash(Consumer<byte[]> h, Hashable... hashables) {
        hash(h, Arrays.asList(hashables));
    }

    protected final void hash(Consumer<byte[]> hash, long... values) {
        for (long value : values) {
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(value);
            hash.accept(buffer.array());
        }
    }

    protected final void hash(Consumer<byte[]> h, Collection<? extends Hashable> hasheds) {
        for (Hashable hashable : hasheds) {
            if (hashable != null) {
                hashable.hashTo(h);
            }
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

    private static final String HASH = "MD5";

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
