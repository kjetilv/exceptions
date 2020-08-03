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

package unearth.munch.model;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import unearth.munch.print.CauseFrame;
import unearth.munch.print.ConfigurableStackRenderer;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class FrameFun {

    public static final ConfigurableStackRenderer.FrameTransform UNSET_MODULE_INFO = CauseFrame::unsetModuleInfo;

    public static final ConfigurableStackRenderer.FrameTransform UNSET_CLASSLOADER = CauseFrame::unsetClassLoader;

    public static final ConfigurableStackRenderer.FrameTransform LIKE_JAVA_8 =
        UNSET_MODULE_INFO.andThen(UNSET_CLASSLOADER)::apply;

    public static final ConfigurableStackRenderer.FrameLister JUST_COUNT =
        FrameFun::justTheCount;

    public static final ConfigurableStackRenderer.FrameLister JUST_COUNT_AND_TOP =
        FrameFun::justTheCountAndTop;

    public static final ConfigurableStackRenderer.GroupedFrameTransform SHORTEN_CLASSNAME =
        FrameFun::shortenClassName;

    public static final ConfigurableStackRenderer.FrameTransform SHORTEN_CLASSNAMES = CauseFrame::shortenClassName;

    private FrameFun() {
    }

    public static CauseFrame shortenClassName(Collection<String> group, CauseFrame causeFrame) {
        return group == null ? causeFrame : causeFrame.shortenClassName();
    }

    private static Stream<String> justTheCount(Collection<String> group, List<CauseFrame> causeFrames) {
        return Stream.of(" * (" + causeFrames.size() + ")");
    }

    private static Stream<String> justTheCountAndTop(Collection<String> group, List<CauseFrame> causeFrames) {
        return Stream.of(
            causeFrames.iterator().next().defaultPrint(new StringBuilder()).toString(),
            " * (" + (causeFrames.size() - 1) + " more)");
    }
}
