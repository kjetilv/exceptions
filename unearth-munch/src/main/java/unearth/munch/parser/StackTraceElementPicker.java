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

interface StackTraceElementPicker {
    
    default String module(String... parts) {
        return null;
    }
    
    default String moduleVersion(String... parts) {
        return null;
    }
    
    default String className(String... parts) {
        return null;
    }
    
    default String method(String... parts) {
        return null;
    }
    
    default String file(String... parts) {
        return null;
    }
    
    default String otherSource(String... parts) {
        return null;
    }
    
    default Integer lineNo(String... parts) {
        return null;
    }
    
    default Integer more(String... parts) {
        return null;
    }
}
