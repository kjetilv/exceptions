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

import no.scienta.unearth.munch.model.CauseChain;
import no.scienta.unearth.munch.util.Streams;
import no.scienta.unearth.munch.util.Util;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfigurableThrowableRenderer implements ThrowableRenderer {

    private final List<GroupedFrameTransform> reshapers;

    private final GroupPrinter groupPrinter;

    private final FramePrinter framePrinter;

    private final PackageGrouper grouper;

    private final FrameLister squasher;

    private final boolean omitStack;

    public ConfigurableThrowableRenderer() {
        this(null, null, null, null, null, false);
    }

    private ConfigurableThrowableRenderer(
        PackageGrouper grouper,
        GroupPrinter groupDisplay,
        FramePrinter framePrinter,
        List<GroupedFrameTransform> reshapers,
        FrameLister squasher,
        boolean omitStack
    ) {
        this.grouper = grouper == null
            ? causeFrame -> Optional.empty()
            : grouper;
        this.groupPrinter = groupDisplay == null
            ? (group, size) -> group
            : groupDisplay;
        this.reshapers = Util.orEmpty(reshapers);
        this.framePrinter = framePrinter == null
            ? (sb, cf) -> cf.defaultPrint(sb)
            : framePrinter;
        this.squasher = squasher;
        this.omitStack = omitStack;
    }

    public ThrowableRenderer framePrinter(FramePrinter framePrinter) {
        return new ConfigurableThrowableRenderer(
            grouper, groupPrinter, framePrinter, reshapers, squasher, omitStack);
    }

    public ConfigurableThrowableRenderer group(PackageGrouper grouper) {
        return new ConfigurableThrowableRenderer(
            grouper, groupPrinter, framePrinter, reshapers, squasher, omitStack);
    }

    public ConfigurableThrowableRenderer squash(FrameLister squasher) {
        return new ConfigurableThrowableRenderer(
            grouper, groupPrinter, framePrinter, reshapers, squasher, omitStack);
    }

    public final ConfigurableThrowableRenderer reshape(GroupedFrameTransform... reshapers) {
        return new ConfigurableThrowableRenderer(
            grouper, groupPrinter, framePrinter, added(reshapers), squasher, omitStack);
    }

    public final ConfigurableThrowableRenderer noStack() {
        return new ConfigurableThrowableRenderer(
            grouper, groupPrinter, framePrinter, reshapers, squasher, true);
    }

    public final ConfigurableThrowableRenderer reshape(FrameTransform... reshapers) {
        List<GroupedFrameTransform> added =
            added(Arrays.stream(reshapers).map(ConfigurableThrowableRenderer::ungrouped));
        return new ConfigurableThrowableRenderer(
            grouper, groupPrinter, framePrinter, added, squasher, omitStack);
    }

    @Override
    public List<String> render(CauseChain causeChain) {
        if (omitStack) {
            return Collections.emptyList();
        }
        List<CauseFrame> causeFrames = causeChain.getCauseFrames();
        GroupedList<Collection<String>, CauseFrame> groupedList =
            GroupedList.group(causeFrames, grouper);
        List<String> list = new ArrayList<>();
        groupedList.forEach((names, frames) -> {
            boolean grouped = names != null;
            if (grouped) {
                list.addAll(printGroupHeading(names, names.size()));
            }
            if (grouped && squasher != null) {
                squashed(names, frames)
                    .forEach(list::add);
            } else {
                frames.stream()
                    .map(reshape(names))
                    .map(item ->
                        print(grouped, item))
                    .forEach(list::add);
            }
        });
        return list;
    }

    private Stream<String> squashed(Collection<String> names, List<CauseFrame> frames) {
        return squasher.apply(names, frames);
    }

    private Function<CauseFrame, CauseFrame> reshape(Collection<String> group) {
        return causeFrame ->
            Streams.quickReduce(reshapers, causeFrame, (cf, reshaper) ->
                reshaper.apply(group, cf));
    }

    private List<GroupedFrameTransform> added(GroupedFrameTransform... reshapers) {
        return added(Arrays.stream(reshapers));
    }

    private List<GroupedFrameTransform> added(Stream<GroupedFrameTransform> reshapers) {
        return Stream.concat(this.reshapers.stream(), reshapers).collect(Collectors.toList());
    }

    private String print(boolean grouped, CauseFrame item) {
        return indented(grouped, framePrinter.apply(new StringBuilder(), item).toString());
    }

    private Collection<String> printGroupHeading(Collection<String> group, int groupSize) {
        String groupString = group.size() > 1 ? String.join("/", group) : group.iterator().next();
        String printedGroup = groupPrinter.apply(groupString, groupSize);
        return Collections.singleton(indented(false, printedGroup));
    }

    private String indented(boolean indent, String printed) {
        return (indent ? INDENT : "") + printed;
    }

    private static final String INDENT = "  ";

    private static GroupedFrameTransform ungrouped(Function<CauseFrame, CauseFrame> fun) {
        return (group, causeFrame) -> fun.apply(causeFrame);
    }

    public interface FramePrinter extends BiFunction<StringBuilder, CauseFrame, StringBuilder> {
    }

    public interface FrameLister extends BiFunction<Collection<String>, List<CauseFrame>, Stream<String>> {
    }

    public interface FrameTransform extends Function<CauseFrame, CauseFrame> {
    }

    public interface GroupedFrameTransform extends BiFunction<Collection<String>, CauseFrame, CauseFrame> {
    }

    public interface PackageGrouper extends Function<CauseFrame, Optional<Collection<String>>> {
    }

    public interface GroupPrinter extends BiFunction<String, Integer, String> {
    }
}
