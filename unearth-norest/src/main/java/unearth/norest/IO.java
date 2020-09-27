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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface IO {

    byte[] writeBytes(ContentType contentType, Object value);

    Object readBytes(ContentType contentType, Class<?> type, InputStream inputStream);

    enum ContentType {

        APPLICATION_JSON,

        TEXT_PLAIN;

        public String withCharset() {
            return withCharset(null);
        }

        public String withCharset(Charset charset) {
            return name() + ";charset=" + (charset == null ? StandardCharsets.UTF_8 : charset).name();
        }

        @Override
        public String toString() {
            return name().toLowerCase().replace('_', '/');
        }
    }
}
