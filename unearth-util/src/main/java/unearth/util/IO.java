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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class IO {
    
    public static String readPath(String name) {
        return readPath(null, name);
    }
    
    public static String readPath(ClassLoader classLoader, String name) {
        byte[] buffer = new byte[8192];
        ClassLoader cl = classLoader == null
            ? Thread.currentThread().getContextClassLoader()
            : classLoader;
        try (InputStream in = cl.getResourceAsStream(name)) {
            if (in == null) {
                return null;
            }
            InputStream bin = new BufferedInputStream(in);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                while (true) {
                    int read = bin.read(buffer);
                    if (read > 0) {
                        out.write(buffer, 0, read);
                    } else if (read < 0) {
                        return new String(out.toByteArray(), StandardCharsets.UTF_8);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + name, e);
        }
    }
    
    private IO() {
    }
}
