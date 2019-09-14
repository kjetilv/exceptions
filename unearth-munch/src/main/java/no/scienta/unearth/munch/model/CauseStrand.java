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

import no.scienta.unearth.munch.ChameleonException;
import no.scienta.unearth.munch.base.AbstractHashableIdentifiable;
import no.scienta.unearth.munch.id.CauseStrandId;
import no.scienta.unearth.munch.print.CauseFrame;
import no.scienta.unearth.util.Util;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A cause strand consists of a stacktrace and an exception class name.
 */
public final class CauseStrand extends AbstractHashableIdentifiable<CauseStrandId> {

    private final String className;

    private final List<CauseFrame> causeFrames;

    private CauseStrand(String className, List<CauseFrame> stackFrames) {
        this.className = className;
        this.causeFrames = Util.orEmpty(stackFrames);
    }

    public static CauseStrand create(Throwable throwable) {
        return new CauseStrand(className(throwable), causeFrames(throwable.getStackTrace()));
    }

    public static CauseStrand create(String className, List<CauseFrame> stackFrames) {
        return new CauseStrand(className, stackFrames);
    }

    public List<CauseFrame> getCauseFrames() {
        return causeFrames;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hash(h, this.className);
        hash(h, this.causeFrames);
    }

    @Override
    protected CauseStrandId id(UUID hash) {
        return new CauseStrandId(hash);
    }

    @Override
    protected String toStringBody() {
        int dotIndex = className.lastIndexOf(".");
        return (dotIndex >= 0 ? className.substring(dotIndex + 1) : className) + '/' + causeFrames.size();
    }

    private static List<CauseFrame> causeFrames(StackTraceElement[] stackTrace) {
        return Arrays.stream(stackTrace).map(ste -> new CauseFrame(
            CauseFrame.ClassLoader(ste.getClassLoaderName()),
            CauseFrame.Module(ste.getModuleName()),
            CauseFrame.ModuleVer(ste.getModuleVersion()),
            CauseFrame.ClassName(ste.getClassName()),
            CauseFrame.Method(ste.getMethodName()),
            CauseFrame.File(ste.getFileName()),
            ste.getLineNumber(),
            ste.isNativeMethod()
        )).collect(Collectors.toUnmodifiableList());
    }

    private static String className(Throwable throwable) {
        return throwable instanceof ChameleonException
            ? ((ChameleonException) throwable).getProxiedClassName()
            : throwable.getClass().getName();
    }
}
