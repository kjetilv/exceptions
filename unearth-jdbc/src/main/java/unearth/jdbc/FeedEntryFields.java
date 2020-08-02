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

package unearth.jdbc;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import unearth.util.once.Once;

enum FeedEntryFields {
    
    FAULT,
    
    FAULT_STRAND,
    
    TIME,
    
    GLOBAL_SEQ,
    
    FAULT_STRAND_SEQ,
    
    FAULT_SEQ;
    
    static String list() {
        return FIELDS.get();
    }
    
    private static final Supplier<String> FIELDS = Once.mostly(() ->
        Arrays.stream(values()).map(Enum::name).map(String::toLowerCase).collect(Collectors.joining(", ")));
}
