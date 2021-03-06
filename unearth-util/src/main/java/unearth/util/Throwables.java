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
package unearth.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Collectors;

public final class Throwables {

    public static ByteBuffer byteBuffer(Throwable throwable) {
        return ByteBuffer.wrap(bytes(throwable));
    }

    public static String toStringString(Throwable throwable) {
        return Streams.causes(throwable).map(Throwable::toString).collect(Collectors.joining("\n => "));
    }

    public static String string(Throwable throwable) {
        return new String(bytes(throwable), StandardCharsets.UTF_8);
    }

    public static String join(Throwable e, String sep) {
        return Streams.causes(e).map(Objects::toString).collect(Collectors.joining(sep));
    }

    private Throwables() {
    }

    private static byte[] bytes(Throwable throwable) {
        Objects.requireNonNull(throwable);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (PrintWriter pw = new PrintWriter(baos)) {
                throwable.printStackTrace(pw);
            }
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize: " + throwable, e);
        }
    }
}
