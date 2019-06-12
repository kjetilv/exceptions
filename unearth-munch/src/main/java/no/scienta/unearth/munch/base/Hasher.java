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

import java.security.MessageDigest;
import java.util.UUID;
import java.util.function.Supplier;

final class Hasher {

    private static final String HASH = "MD5";

    /**
     * Takes a {@link Hashable hashable} and returns a supplier which computs its UUID
     * @param hashable Hashable
     * @return UUID supplier
     */
    static Supplier<UUID> uuid(Hashable hashable) {
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

    private Hasher() {
    }
}
