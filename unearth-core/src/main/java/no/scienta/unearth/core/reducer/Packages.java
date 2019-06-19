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

package no.scienta.unearth.core.reducer;

import no.scienta.unearth.munch.model.CauseFrame;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Packages implements Predicate<CauseFrame>, UnaryOperator<CauseFrame> {

    public static Packages all() {
        return new Packages(null);
    }

    public static Packages none() {
        return new Packages(null, true);
    }

    public static Packages shortened(String... packages) {
        return new Packages(Packages::shorten, packages);
    }

    public static Packages moderateShortened(String... packages) {
        return new Packages(Packages::moderateShorten, packages);
    }

    public static Packages removed(String... packages) {
        return new Packages(Packages::shorten, packages);
    }

    private final boolean none;

    private final List<String> prefixes;

    private final Map<String, String> shortPrefixes;

    private Packages(UnaryOperator<String> shorten, String... prefixes) {
        this(shorten, false, prefixes);
    }

    private Packages(UnaryOperator<String> shorten, boolean none, String... prefixes) {
        this.prefixes = prefixes.length == 0 ? Collections.emptyList() : Arrays.stream(prefixes)
            .distinct()
            .sorted(Comparator.comparing(String::length).reversed())
            .collect(Collectors.toList());
        this.shortPrefixes = shorten == null
            ? Collections.emptyMap()
            : this.prefixes.stream().collect(Collectors.toMap(Function.identity(), shorten));
        this.none = none;
    }

    private static String shorten(String prefix) {
        return shorten(prefix, false);
    }

    private static String moderateShorten(String prefix) {
        return shorten(prefix, true);
    }

    private static String shorten(String prefix, boolean grow) {
        String[] parts = prefix.split("\\.");
        return IntStream.range(0, parts.length)
            .mapToObj(i ->
                parts[i].substring(0, grow
                    ? Math.min(parts[i].length(), i + 1)
                    : 1))
            .collect(Collectors.joining("."));
    }

    @Override
    public boolean test(CauseFrame stackTraceElement) {
        return !none && (
            prefixes.isEmpty() || prefixes.stream().anyMatch(stackTraceElement.className()::startsWith)
        );
    }

    @Override
    public CauseFrame apply(CauseFrame ste) {
        if (shortPrefixes.isEmpty()) {
            return ste;
        }
        return Optional.of(ste.className())
            .flatMap(this::packageName)
            .flatMap(packageName ->
                shortPrefixes.entrySet().stream()
                    .filter(e ->
                        packageName.startsWith(e.getKey()))
                    .findFirst())
            .map(e ->
                new CauseFrame(
                    ste.classLoader(),
                    ste.module(),
                    ste.moduleVer(),
                    e.getValue() + ste.className().substring(e.getKey().length()),
                    ste.method(),
                    ste.file(),
                    ste.line(),
                    ste.naytiv()))
            .orElse(ste);
    }

    private Optional<String> packageName(String name) {
        int endIndex = name.lastIndexOf(".");
        return endIndex > 0 ? Optional.of(name.substring(0, endIndex)) : Optional.empty();
    }
}
