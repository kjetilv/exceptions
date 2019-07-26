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

package no.scienta.unearth.munch.model;

import no.scienta.unearth.munch.print.CauseFrame;
import no.scienta.unearth.munch.print.ConfigurableThrowableRenderer;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class FrameFun {

    public static ConfigurableThrowableRenderer.FrameTransform UNSET_MODULE_INFO = CauseFrame::unsetModuleInfo;

    public static ConfigurableThrowableRenderer.FrameTransform UNSET_CLASSLOADER = CauseFrame::unsetClassLoader;

    public static ConfigurableThrowableRenderer.FrameTransform LIKE_JAVA_8 = UNSET_MODULE_INFO.andThen(UNSET_CLASSLOADER)::apply;

    public static ConfigurableThrowableRenderer.FrameLister JUST_COUNT =
        FrameFun::justTheCount;

    public static ConfigurableThrowableRenderer.FrameLister JUST_COUNT_AND_TOP =
        FrameFun::justTheCountAndTop;

    public static ConfigurableThrowableRenderer.GroupedFrameTransform SHORTEN_CLASSNAME =
        FrameFun::shortenClassName;

    public static ConfigurableThrowableRenderer.FrameTransform SHORTEN_CLASSNAMES = FrameFun::shortenClassName;

    private static Stream<String> justTheCount(Collection<String> group, List<CauseFrame> causeFrames) {
        return Stream.of("  * (" + causeFrames.size() + ")");
    }

    private static Stream<String> justTheCountAndTop(Collection<String> group, List<CauseFrame> causeFrames) {
        return Stream.of(
            causeFrames.iterator().next().defaultPrint(new StringBuilder()).toString(),
            "  * (" + (causeFrames.size() - 1) + " more)");
    }

    public static CauseFrame shortenClassName(Collection<String> group, CauseFrame causeFrame) {
        return group == null ? causeFrame : shortenClassName(causeFrame);
    }

    private static CauseFrame shortenClassName(CauseFrame causeFrame) {
        return causeFrame.className(shortened(causeFrame.className()));
    }

    private static String shortened(String className) {
        int dot = className.lastIndexOf(".");
        return Stream.concat(
            Arrays.stream(className.substring(0, dot).split("\\."))
                .map(part -> part.substring(0, 1)),
            Stream.of(className.substring(dot + 1))
        ).collect(Collectors.joining("."));
    }

    private FrameFun() {
    }
}
