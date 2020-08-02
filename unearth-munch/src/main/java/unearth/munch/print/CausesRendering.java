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

package unearth.munch.print;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import unearth.util.Streams;

public final class CausesRendering implements Iterable<CausesRendering> {
    
    private final String className;
    
    private final String message;
    
    private final Collection<String> stack;
    
    private final CausesRendering cause;
    
    CausesRendering(String className, String message, Collection<String> stack, CausesRendering cause) {
        this.className = className;
        this.message = message == null || message.isBlank() ? "null" : message.trim();
        this.stack = stack == null || stack.isEmpty() ? Collections.emptyList() : List.copyOf(stack);
        this.cause = cause;
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
    
    public CausesRendering getCause() {
        return cause;
    }
    
    public Collection<String> getStrings(String stackIndent) {
        String indent = stackIndent == null ? DEFAULT_INDENT : stackIndent;
        return Stream.concat(
            Stream.of(className + ": " + message),
            stack.stream().map(line -> indent + line)
        ).collect(Collectors.toList());
    }
    
    @Override
    public Iterator<CausesRendering> iterator() {
        return Streams.chain(this, CausesRendering::getCause).iterator();
    }
    
    private static final String DEFAULT_INDENT = "\t";
}
