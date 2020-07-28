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

package unearth.munch.print;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import org.junit.Test;
import unearth.munch.model.Fault;
import unearth.munch.model.FrameFun;

public class StackTraceRewriterTest {

    @Test
    public void test() {
        StackRenderer causeChainRenderer = new ConfigurableStackRenderer()
            .group(new SimplePackageGrouper(
                Arrays.asList(
                    Collections.singleton("org.gradle"),
                    Collections.singleton("org.junit"),
                    Arrays.asList("java", "jdk", "com.sun"))))
            .squash((group, causeFrames) ->
                Stream.of(" * (" + causeFrames.size() + ")"))
            .reshape(
                CauseFrame::unsetClassLoader,
                CauseFrame::unsetModuleInfo)
            .reshape(
                FrameFun::shortenClassName)
            .framePrinter((sb, cf) ->
                cf.defaultPrint(sb.append("--  ")));

        Fault fault = Fault.create(new Throwable());
        CausesRendering render = new SimpleCausesRenderer(causeChainRenderer).render(fault);

        render.getStrings("##  ").forEach(System.out::println);
    }
}
