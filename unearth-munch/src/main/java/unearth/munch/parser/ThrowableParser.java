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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class ThrowableParser {
    
    static final String SUPPRESSED = "Suppressed: ";
    
    public static Throwable parse(ByteBuffer buffer) {
        return parse(new String(buffer.array(), StandardCharsets.UTF_8));
    }
    
    public static Throwable parse(String in) {
        Objects.requireNonNull(in, "in");
        if (in.contains(SUPPRESSED)) {
            return WellformedThrowableParser.parse(in);
        }
        try {
            return TolerantThrowableParser.parse(in);
        } catch (Exception e) {
            return failedParse(in, e);
        }
    }
    
    private ThrowableParser() {
    }
    
    private static <T> T failedParse(String in, Exception e) {
        
        throw new IllegalArgumentException(
            "Failed to parse as exception: " + in.substring(0, Math.min(30, in.length())) + "...", e);
    }
}
