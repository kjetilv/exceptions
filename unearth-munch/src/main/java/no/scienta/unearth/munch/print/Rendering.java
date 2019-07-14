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

package no.scienta.unearth.munch.print;

import no.scienta.unearth.munch.util.Util;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Rendering {

    private static final String DEFAULT_INDENT = "\t";
    private final String className;

    private final String message;

    private final Collection<String> stack;

    public Rendering(String className, String message, Collection<String> stack) {
        this.className = className;
        this.message = message == null || message.trim().isEmpty() ? "null" : message.trim();
        this.stack = Util.orEmpty(stack);
    }

    public String getClassName() {
        return className;
    }

    public String getMessage() {
        return message;
    }

    public Collection<String> getStack() {
        return stack;
    }

    public Collection<String> getStrings() {
        return getStrings(null);
    }

    public Collection<String> getStrings(String stackIndent) {
        String indent = stackIndent == null ? DEFAULT_INDENT : stackIndent;
        return Stream.concat(
            Stream.of(className + ": " + message),
            stack.stream().map(line -> indent + line)
        ).collect(Collectors.toList());
    }
}
