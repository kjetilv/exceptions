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
import no.scienta.unearth.munch.model.CauseChain;
import no.scienta.unearth.munch.model.CauseFrame;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CauseChainRenderer implements Function<CauseChain, List<String>> {

    public static BiFunction<Collection<String>, CauseFrame, CauseFrame> SHORTEN_CLASSNAME =
        CauseChainRenderer::shortenClassname;

    public static Function<CauseFrame, CauseFrame> SHORTEN_ALL_CLASSNAME =
        CauseChainRenderer::shortenClassName;

    public static CauseFrame shortenClassname(Collection<String> group, CauseFrame causeFrame) {
        return group == null ? causeFrame : shortenClassName(causeFrame);
    }

    private static CauseFrame shortenClassName(CauseFrame causeFrame) {
        String className = causeFrame.className();
        int dot = className.lastIndexOf(".");
        String shortened = Stream.concat(
            Arrays.stream(className.substring(0, dot).split("\\."))
                .map(part -> part.substring(0, 1)),
            Stream.of(className.substring(dot + 1)))
            .collect(Collectors.joining("."));
        return causeFrame.className(shortened);
    }

    private final List<BiFunction<Collection<String>, CauseFrame, CauseFrame>> reshapers;

    private final BiFunction<String, Integer, String> groupPrinter;

    private final BiFunction<StringBuilder, CauseFrame, StringBuilder> framePrinter;

    private final Function<CauseFrame, Optional<Collection<String>>> grouper;

    private final BiFunction<Collection<String>, List<CauseFrame>, Optional<String>> squasher;

    public CauseChainRenderer() {
        this(null, null, null, null, null);
    }

    private CauseChainRenderer(
        Function<CauseFrame, Optional<Collection<String>>> grouper,
        BiFunction<String, Integer, String> groupDisplay,
        BiFunction<StringBuilder, CauseFrame, StringBuilder> framePrinter,
        List<BiFunction<Collection<String>, CauseFrame, CauseFrame>> reshapers,
        BiFunction<Collection<String>, List<CauseFrame>, Optional<String>> squasher
    ) {
        this.grouper = grouper == null
            ? causeFrame -> Optional.empty()
            : grouper;
        this.groupPrinter = groupDisplay == null
            ? (group, size) -> group
            : groupDisplay;
        this.reshapers = reshapers == null || reshapers.isEmpty()
            ? Collections.emptyList()
            : List.copyOf(reshapers);
        this.framePrinter = framePrinter == null
            ? (sb, cf) -> cf.defaultPrint(sb)
            : framePrinter;
        this.squasher = squasher;
    }

    public CauseChainRenderer groupPrinter(BiFunction<String, Integer, String> groupDisplay) {
        return new CauseChainRenderer(grouper, groupPrinter, framePrinter, reshapers, squasher);
    }

    public CauseChainRenderer framePrinter(BiFunction<StringBuilder, CauseFrame, StringBuilder> framePrinter) {
        return new CauseChainRenderer(grouper, groupPrinter, framePrinter, reshapers, squasher);
    }

    public CauseChainRenderer group(Function<CauseFrame, Optional<Collection<String>>> grouper) {
        return new CauseChainRenderer(grouper, groupPrinter, framePrinter, reshapers, squasher);
    }

    public CauseChainRenderer squasher(BiFunction<Collection<String>, List<CauseFrame>, Optional<String>> squasher) {
        return new CauseChainRenderer(grouper, groupPrinter, framePrinter, reshapers, squasher);
    }

    @SafeVarargs
    public final CauseChainRenderer reshape(BiFunction<Collection<String>, CauseFrame, CauseFrame>... reshapers) {
        return new CauseChainRenderer(grouper, groupPrinter, framePrinter, added(Arrays.stream(reshapers)), squasher);
    }

    @SafeVarargs
    public final CauseChainRenderer reshapeAll(Function<CauseFrame, CauseFrame>... reshapers) {
        List<BiFunction<Collection<String>, CauseFrame, CauseFrame>> added =
            added(Arrays.stream(reshapers).map(CauseChainRenderer::all));
        return new CauseChainRenderer(grouper, groupPrinter, framePrinter, added, squasher);
    }

    @Override
    public List<String> apply(CauseChain causeChain) {
        List<CauseFrame> causeFrames = causeChain.getCauseFrames();
        GroupedList<Collection<String>, CauseFrame> groupedList =
            GroupedList.group(causeFrames, grouper);
        List<String> list = new ArrayList<>();
        groupedList.forEach((names, frames) -> {
            if (names != null) {
                list.add(printGroup(names, names.size()));
            }
            if (names == null || squasher == null) {
                frames.stream()
                    .map(reshape(names))
                    .map(item ->
                        print(names != null, item)).forEach(list::add);
            } else {
                squasher.apply(names, frames).ifPresent(list::add);
            }
        });
        return list;
    }

    private Function<CauseFrame, CauseFrame> reshape(Collection<String> group) {
        return causeFrame -> reshapers.stream().reduce(
            causeFrame,
            (cf, reshaper) -> reshaper.apply(group, cf),
            (cf1, cf2) -> {
                throw new IllegalStateException("No combine: " + cf1 + "/" + cf2);
            });
    }

    private List<BiFunction<Collection<String>, CauseFrame, CauseFrame>> added(
        Stream<BiFunction<Collection<String>, CauseFrame, CauseFrame>> reshapers
    ) {
        return Stream.concat(this.reshapers.stream(), reshapers).collect(Collectors.toList());
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

    private static BiFunction<Collection<String>, CauseFrame, CauseFrame> all(Function<CauseFrame, CauseFrame> fun) {
        return (group, causeFrame) -> fun.apply(causeFrame);
    }
}
