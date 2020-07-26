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
package no.scienta.unearth.statik;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import no.scienta.unearth.util.IO;

public final class Statik {

    private final ClassLoader classLoader;

    private final String prefix;

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public Statik(ClassLoader classLoader, String prefix) {
        this.classLoader = classLoader;
        this.prefix = prefix;
    }

    public Optional<String> read(String path) {
        return Optional.ofNullable(cache.computeIfAbsent(path, this::readPath));
    }

    private String readPath(String path) {
        return IO.readPath(this.classLoader, prefix + path);
    }
}
