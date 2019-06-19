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

import no.scienta.unearth.munch.base.GroupedList;
import no.scienta.unearth.munch.model.CauseFrame;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StackTraceReshaper {

    public static CauseFrame shortenClassname(CauseFrame causeFrame) {
        String className = causeFrame.className();
        int dot = className.lastIndexOf(".");
        String shortened = Stream.concat(
            Arrays.stream(className.substring(0, dot).split("\\."))
                .map(part -> part.substring(0, 1)),
            Stream.of(className.substring(dot + 1)))
            .collect(Collectors.joining("."));
        return causeFrame.className(shortened);
    }

    private final List<Function<CauseFrame, CauseFrame>> reshapers;

    private final BiFunction<String, Integer, String> groupPrinter;

    private final BiFunction<StringBuilder, CauseFrame, StringBuilder> framePrinter;

    private final Function<CauseFrame, Optional<Collection<String>>> grouper;

    private final BiFunction<Collection<String>, List<CauseFrame>, Optional<String>> squasher;

    public static StackTraceReshaper create() {
        return new StackTraceReshaper(null, null, null, null, null);
    }

    private StackTraceReshaper(
        Function<CauseFrame, Optional<Collection<String>>> grouper,
        BiFunction<String, Integer, String> groupDisplay,
        BiFunction<StringBuilder, CauseFrame, StringBuilder> framePrinter,
        List<Function<CauseFrame, CauseFrame>> reshapers,
        BiFunction<Collection<String>, List<CauseFrame>, Optional<String>> squasher
    ) {
        this.grouper = grouper == null
            ? causeFrame -> Optional.empty()
            : grouper;
        this.groupPrinter = groupDisplay == null
            ? (group, size) -> group + ":" + size
            : groupDisplay;
        this.reshapers = reshapers == null || reshapers.isEmpty()
            ? Collections.emptyList()
            : List.copyOf(reshapers);
        this.framePrinter = framePrinter == null
            ? (sb, cf) -> cf.defaultPrint(sb)
            : framePrinter;
        this.squasher = squasher;
    }

    public StackTraceReshaper groupPrinter(BiFunction<String, Integer, String> groupDisplay) {
        return new StackTraceReshaper(grouper, groupPrinter, framePrinter, reshapers, squasher);
    }

    public StackTraceReshaper framePrinter(BiFunction<StringBuilder, CauseFrame, StringBuilder> framePrinter) {
        return new StackTraceReshaper(grouper, groupPrinter, framePrinter, reshapers, squasher);
    }

    public StackTraceReshaper group(Function<CauseFrame, Optional<Collection<String>>> grouper) {
        return new StackTraceReshaper(grouper, groupPrinter, framePrinter, reshapers, squasher);
    }

    public StackTraceReshaper squasher(BiFunction<Collection<String>, List<CauseFrame>, Optional<String>> squasher) {
        return new StackTraceReshaper(grouper, groupPrinter, framePrinter, reshapers, squasher);
    }

    @SafeVarargs
    public final StackTraceReshaper reshape(Function<CauseFrame, CauseFrame>... reshapers) {
        return new StackTraceReshaper(
            grouper,
            groupPrinter,
            framePrinter,
            added(reshapers),
            squasher);
    }

    private CauseFrame reshape(CauseFrame causeFrame) {
        return reshapers.stream().reduce(
            causeFrame,
            (cf, reshaper) -> reshaper.apply(cf),
            (cf1, cf2) -> {
                throw new IllegalStateException("No combine: " + cf1 + "/" + cf2);
            });
    }

    public List<String> prettified(List<CauseFrame> stackTrace) {
        GroupedList<Collection<String>, CauseFrame> groupedList =
            GroupedList.group(stackTrace, grouper);
        List<String> list = new ArrayList<>();
        groupedList.forEach((names, causeFrames) -> {
            if (names != null) {
                list.add(printGroup(names, names.size()));
            }
            if (names == null || squasher == null) {
                causeFrames.stream()
                    .map(this::reshape)
                    .map(item ->
                        print(names != null, item)).forEach(list::add);
            } else {
                squasher.apply(names, causeFrames).ifPresent(list::add);
            }
        });
        return list;
    }

    private List<Function<CauseFrame, CauseFrame>> added(Function<CauseFrame, CauseFrame>[] reshapers) {
        return Stream.concat(this.reshapers.stream(), Arrays.stream(reshapers)).collect(Collectors.toList());
    }

    private String print(boolean grouped, CauseFrame item) {
        return indented(grouped, framePrinter.apply(new StringBuilder(), item).toString());
    }

    private String printGroup(Collection<String> group, int groupSize) {
        String groupString = group.size() > 1 ? String.join("/", group) : group.iterator().next();
        String printedGroup = groupPrinter.apply(groupString, groupSize);
        return indented(false, printedGroup);
    }

    private String indented(boolean indent, String printed) {
        return (indent ? INDENT : "") + printed;
    }

    private static final String INDENT = "  ";
}
