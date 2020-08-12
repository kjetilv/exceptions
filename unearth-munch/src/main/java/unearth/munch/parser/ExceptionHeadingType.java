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

package unearth.munch.parser;

import java.util.regex.Pattern;

public enum ExceptionHeadingType implements ExceptionHeadingPicker {
    
    SUPPRESSED("Suppressed"),
    
    CAUSE("Caused by"),
    
    MAIN("") {
        @Override
        public String type(String... parts) {
            return parts[0];
        }
        
        @Override
        public String message(String... parts) {
            return parts[1];
        }
        
        @Override
        public String[] toParts(String line) {
            return COLON.split(line, 2);
        }
    };
    
    private final String prefix;
    
    ExceptionHeadingType(String prefix) {
        this.prefix = prefix;
    }
    
    public String[] toParts(String line) {
        return line.startsWith(prefix) ? COLON.split(line, 3) : null;
    }
    
    @Override
    public String type(String... parts) {
        return parts[1];
    }
    
    @Override
    public String message(String... parts) {
        return parts.length > 2 ? parts[2] : null;
    }
    
    private static final Pattern COLON = Pattern.compile(": ");
}
