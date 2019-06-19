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

import no.scienta.unearth.core.FaultReducer;
import no.scienta.unearth.munch.model.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SimpleFaultReducer implements FaultReducer {

    private final Packages display;

    private final Packages aggregate;

    private final Packages remove;

    public SimpleFaultReducer(Packages display, Packages aggregate, Packages remove) {
        this.display = display;
        this.aggregate = aggregate;
        this.remove = remove;
    }

    @Override
    public FaultStrand reduce(FaultStrand faultStrand) {
        return faultStrand.withCauseStrands(
            faultStrand.getCauseStrands().stream().map(this::reduce).collect(Collectors.toList()));
    }

    @Override
    public Fault reduce(Fault fault) {
        return fault.withCauses(
            fault.getCauses().stream().map(this::reduce).collect(Collectors.toList()));
    }

    @Override
    public Cause reduce(Cause cause) {
        return cause.withCauseStrand(
            reduce(cause.getCauseStrand()));
    }

    @Override
    public CauseStrand reduce(CauseStrand causeStrand) {
        return causeStrand.withCauseFrames(reduce(causeStrand.getCauseFrames()));
    }

    private List<CauseFrame> reduce(List<CauseFrame> stack) {
        List<CauseFrame> reducedStack = new ArrayList<>(stack.size());
        List<CauseFrame> aggregated = new ArrayList<>();
        boolean aggregating = false;
        for (CauseFrame element : stack) {
            if (display != null && display.test(element)) {
                if (aggregating) {
                    reducedStack.addAll(aggregateElements(aggregated));
                    aggregated.clear();
                    aggregating = false;
                }
                reducedStack.add(display.apply(element));
            } else if (remove != null && remove.test(element)) {
                if (aggregating) {
                    reducedStack.addAll(aggregateElements(aggregated));
                    aggregated.clear();
                    aggregating = false;
                }
            } else if (aggregate != null && aggregate.test(element)) {
                aggregating = true;
                aggregated.add(aggregate.apply(element));
            } else {
                reducedStack.add(element);
            }
        }
        return reducedStack;
    }

    private Collection<CauseFrame> aggregateElements(List<CauseFrame> aggregated) {
        return Collections.singleton(
            new CauseFrame(
                null,
                null,
                null,
                commonPrefix(aggregated) + ".*",
                "[" + aggregated.size() + " calls]",
                null,
                -1,
                false));
    }

    private String commonPrefix(List<CauseFrame> aggregated) {
        return aggregated.stream().map(CauseFrame::className).reduce(this::commonPrefix).orElse("");
    }

    private String commonPrefix(String p1, String p2) {
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < Math.min(p1.length(), p2.length()); i++) {
            if (p1.charAt(i) == p2.charAt(i)) {
                prefix.append(p1.charAt(i));
            } else {
                return prefix.toString();
            }
        }
        return prefix.toString();
    }
}
