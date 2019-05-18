package link.stuf.exceptions.core.digest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

public class ThrowableReducer implements UnaryOperator<ThrowableDigest> {

    private final Packages display;

    private final Packages aggregate;

    private final Packages remove;

    public ThrowableReducer(Packages display, Packages aggregate, Packages remove) {
        this.display = display;
        this.aggregate = aggregate;
        this.remove = remove;
    }

    @Override
    public ThrowableDigest apply(ThrowableDigest digest) {
        return digest.withStacktrace(reducedStackTrace(digest));
    }

    private List<StackTraceElement> reducedStackTrace(ThrowableDigest current) {
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
