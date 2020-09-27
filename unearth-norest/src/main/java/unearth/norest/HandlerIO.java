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

package unearth.norest;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

public class HandlerIO implements IO {

    private final Map<ContentType, IOHandler> handlers;

    public HandlerIO(Map<ContentType, IOHandler> handlers) {
        this.handlers = Map.copyOf(Objects.requireNonNull(handlers, "handlers"));
        if (this.handlers.isEmpty()) {
            throw new IllegalArgumentException("No handlers");
        }
    }

    @Override
    public byte[] writeBytes(ContentType contentType, Object value) {
        return handler(contentType).writeBytes(value);
    }

    @Override
    public Object readBytes(ContentType contentType, Class<?> type, InputStream inputStream) {
        return handler(contentType).readBytes(type, inputStream);
    }

    private IOHandler handler(ContentType contentType) {
        IOHandler ioHandler = handlers.get(contentType);
        if (ioHandler == null) {
            throw new IllegalArgumentException("Unknown: " + contentType);
        }
        return ioHandler;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + handlers.keySet() + "]";
    }
}
