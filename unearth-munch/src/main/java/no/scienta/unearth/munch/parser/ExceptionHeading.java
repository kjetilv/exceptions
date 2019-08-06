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

package no.scienta.unearth.munch.parser;

final class ExceptionHeading {

    private final String name;

    private final String message;

    ExceptionHeading(String name, String message) {
        this.name = name;
        this.message = message;
    }

    String getName() {
        return name;
    }

    String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + name + ": " + message + "]";
    }
}
