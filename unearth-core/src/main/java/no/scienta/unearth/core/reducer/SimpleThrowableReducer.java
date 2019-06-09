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

import no.scienta.unearth.core.ThrowablesReducer;
import no.scienta.unearth.munch.data.CauseType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SimpleThrowableReducer implements ThrowablesReducer {

    private final Packages display;

    private final Packages aggregate;

    private final Packages remove;

    public SimpleThrowableReducer(Packages display, Packages aggregate, Packages remove) {
        this.display = display;
        this.aggregate = aggregate;
        this.remove = remove;
    }

    @Override
    public CauseType reduce(CauseType digest) {
        return digest.withStacktrace(reducedStackTrace(digest));
    }

    private List<StackTraceElement> reducedStackTrace(CauseType current) {
        List<StackTraceElement> stack = List.copyOf(current.getStackTrace());
        List<StackTraceElement> reducedStack = new ArrayList<>(stack.size());
        List<StackTraceElement> aggregated = new ArrayList<>();
        boolean aggregating = false;
        for (StackTraceElement element : stack) {
            if (display.test(element)) {
                if (aggregating) {
                    reducedStack.addAll(aggregateElements(aggregated));
                    aggregated.clear();
                    aggregating = false;
                }
                reducedStack.add(display.apply(element));
            } else if (remove.test(element)) {
                if (aggregating) {
                    reducedStack.addAll(aggregateElements(aggregated));
                    aggregated.clear();
                    aggregating = false;
                }
            } else if (aggregate.test(element)) {
                aggregating = true;
                aggregated.add(aggregate.apply(element));
            } else {
                reducedStack.add(element);
            }
        }
        return reducedStack;
    }

    private Collection<StackTraceElement> aggregateElements(List<StackTraceElement> aggregated) {
        return Collections.singleton(
            new StackTraceElement(
                commonPrefix(aggregated) + ".*",
                "[" + aggregated.size() + " calls]",
                null,
                -1));
    }

    private String commonPrefix(List<StackTraceElement> aggregated) {
        return aggregated.stream().map(StackTraceElement::getClassName).reduce(this::commonPrefix).orElse("");
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
