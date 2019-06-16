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

package no.scienta.unearth.munch.model;

import no.scienta.unearth.munch.ChameleonException;
import no.scienta.unearth.munch.base.AbstractHashableIdentifiable;
import no.scienta.unearth.munch.id.CauseStrandId;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A cause strand consists of a stacktrace and an exception class name.
 */
public class CauseStrand extends AbstractHashableIdentifiable<CauseStrandId> {

    public static CauseStrand create(Throwable throwable) {
        return new CauseStrand(className(throwable), copy(throwable.getStackTrace()));
    }

    private final String className;

    private final List<StackTraceElement> stackTrace;

    private CauseStrand(String className, List<StackTraceElement> stackTrace) {
        this.className = className;
        this.stackTrace = stackTrace == null || stackTrace.isEmpty()
            ? Collections.emptyList()
            : List.copyOf(stackTrace);
    }

    public List<StackTraceElement> getStackTrace() {
        return stackTrace;
    }

    @Override
    protected CauseStrandId id(UUID hash) {
        return new CauseStrandId(hash);
    }

    public String getClassName() {
        return className;
    }

    public CauseStrand withStacktrace(List<StackTraceElement> stackTrace) {
        return new CauseStrand(className, stackTrace);
    }

    private static List<StackTraceElement> copy(StackTraceElement[] stackTrace) {
        return Arrays.stream(stackTrace).map(orig -> new StackTraceElement(
                        orig.getClassLoaderName(),
                        orig.getModuleName(),
                        orig.getModuleVersion(),
                        orig.getClassName(),
                        orig.getMethodName(),
                        orig.getFileName(),
                        orig.getLineNumber()
                    )).collect(Collectors.toUnmodifiableList());
    }

    private static String className(Throwable throwable) {
        if (throwable instanceof ChameleonException) {
            return ((ChameleonException) throwable).getProxiedClassName();
        }
        return throwable.getClass().getName();
    }

    @Override
    protected String toStringBody() {
        int dotIndex = className.lastIndexOf(".");
        return (dotIndex >= 0 ? className.substring(dotIndex + 1) : className) + " <" + stackTrace.size() + ">";
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        h.accept(this.className.getBytes(StandardCharsets.UTF_8));
        for (StackTraceElement el : this.stackTrace) {
            h.accept(el.toString().getBytes(StandardCharsets.UTF_8));
        }
    }
}
