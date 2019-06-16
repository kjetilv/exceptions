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

package no.scienta.unearth.munch.base;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class SimpleStringListItemPrinter implements ListItemPrinter<String> {

    @Override
    public String print(int levels, String item) {
        return indented(levels * 2, " ", item);
    }

    @Override
    public String printGroup(int level, String group, int groupSize) {
        return indented(level * 2 - 1, " ", "[" + group + " (" + groupSize + ")]");
    }

    private String indented(int times, String indent, String string) {
        return Stream.concat(
            IntStream.range(0, times).mapToObj(i -> indent),
            Stream.of(string)
        ).collect(Collectors.joining());
    }
}
