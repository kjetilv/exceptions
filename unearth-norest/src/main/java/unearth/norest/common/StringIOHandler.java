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

package unearth.norest.common;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import unearth.norest.IOHandler;

public final class StringIOHandler implements IOHandler {

    private static final byte[] NO_BYTES = new byte[0];

    private final Charset charset;

    public StringIOHandler(Charset charset) {
        this.charset = charset == null ? StandardCharsets.UTF_8 : charset;
    }

    @Override
    public byte[] writeBytes(Object value) {
        return value == null
            ? NO_BYTES
            : value.toString().getBytes(charset);
    }

    @Override
    public Object readBytes(Class<?> type, InputStream inputStream) {
        try (Stream<String> lines = new BufferedReader(
            new InputStreamReader(inputStream, charset)).lines()) {
            return lines.collect(Collectors.joining("\n"));
        }
    }
}
