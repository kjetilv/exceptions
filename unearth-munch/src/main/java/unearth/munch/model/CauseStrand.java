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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import unearth.munch.ChameleonException;
import unearth.munch.id.AbstractHashableIdentifiable;
import unearth.munch.id.CauseStrandId;
import unearth.munch.print.CauseFrame;

/**
 * A cause strand consists of a stacktrace and an exception class name.
 */
public final class CauseStrand extends AbstractHashableIdentifiable<CauseStrandId> {
    
    public static CauseStrand create(Throwable throwable) {
        return new CauseStrand(className(throwable), causeFrames(throwable.getStackTrace()));
    }
    
    public static CauseStrand create(String className, List<CauseFrame> stackFrames) {
        return new CauseStrand(className, stackFrames);
    }
    
    private final String className;
    
    private final List<CauseFrame> causeFrames;
    
    private CauseStrand(String className, List<CauseFrame> stackFrames) {
        this.className = className;
        this.causeFrames =
            stackFrames == null || ((Collection<CauseFrame>) stackFrames).isEmpty()
                ? Collections.emptyList()
                : List.copyOf(stackFrames);
    }
    
    @Override
    public void hashTo(Consumer<byte[]> h) {
        hash(h, this.className);
        hashables(h, this.causeFrames);
    }
    
    @Override
    protected CauseStrandId id(UUID hash) {
        return new CauseStrandId(hash);
    }
    
    @Override
    protected StringBuilder withStringBody(StringBuilder sb) {
        int dotIndex = className.lastIndexOf(".");
        return sb.append(dotIndex >= 0 ? className.substring(dotIndex + 1) : className)
            .append('/').append(causeFrames.size());
    }
    
    public List<CauseFrame> getCauseFrames() {
        return causeFrames;
    }
    
    public String getClassName() {
        return className;
    }
    
    private static List<CauseFrame> causeFrames(StackTraceElement[] stackTrace) {
        return Arrays.stream(stackTrace).map(ste -> new CauseFrame(
            CauseFrame.classLoader(ste.getClassLoaderName()),
            CauseFrame.module(ste.getModuleName()),
            CauseFrame.moduleVer(ste.getModuleVersion()),
            CauseFrame.className(ste.getClassName()),
            CauseFrame.method(ste.getMethodName()),
            CauseFrame.file(ste.getFileName()),
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
