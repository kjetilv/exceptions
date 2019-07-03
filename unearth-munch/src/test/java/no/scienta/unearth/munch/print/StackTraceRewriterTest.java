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

import no.scienta.unearth.munch.model.CauseFrame;
import no.scienta.unearth.munch.model.Fault;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

public class StackTraceRewriterTest {

    @Test
    public void test() {
        CauseChainRenderer causeChainRenderer = new CauseChainRenderer()
            .group(new PackageGrouper(
                Arrays.asList(
                    Collections.singleton("org.gradle"),
                    Collections.singleton("org.junit"),
                    Arrays.asList("java", "jdk", "com.sun"))))
            .squasher((group, causeFrames) ->
                Stream.of("  * (" + causeFrames.size() + ")"))
            .reshapeAll(
                CauseFrame::unsetClassLoader,
                CauseFrame::unsetModuleInfo)
            .reshape(
                CauseChainRenderer::shortenClassname)
            .framePrinter((sb, cf) ->
                cf.defaultPrint(sb.append("--  ")));

        Fault fault = Fault.create(new Throwable());
        CauseChain causeChain = CauseChain.build(fault).withPrintout(causeChainRenderer);

        causeChain.getPrintout().forEach(System.out::println);
    }
}
