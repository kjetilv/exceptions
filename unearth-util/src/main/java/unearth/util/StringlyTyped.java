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

import java.util.Objects;

public abstract class StringlyTyped {

    public static String toString(StringlyTyped stringlyTyped) {
        return stringlyTyped == null ? null : stringlyTyped.string();
    }

    private final String string;

    protected StringlyTyped(String string) {
        this.string = norm(string);
    }

    public String string() {
        return string;
    }

    private static String norm(String s) {
        return blank(s) ? "" : s;
    }

    private static boolean blank(String s) {
        return s == null || s.length() == 0 || s.isBlank();
    }

    @Override
    public int hashCode() {
        return string.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return this == o ||
               o != null && getClass() == o.getClass() && Objects.equals(string, ((StringlyTyped) o).string);
    }

    @Override
    public String toString() {
        return string();
    }
}
