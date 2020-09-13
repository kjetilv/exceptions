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
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import unearth.hashable.AbstractHashable;
import unearth.util.StringlyTyped;

public final class CauseFrame extends AbstractHashable {
    
    public static ClassLoader classLoader(String s) {
        return new ClassLoader(s);
    }
    
    public static Module module(String s) {
        return new Module(s);
    }
    
    public static ModuleVer moduleVer(String s) {
        return new ModuleVer(s);
    }
    
    public static ClassName className(String s) {
        return new ClassName(s);
    }
    
    public static Method method(String s) {
        return new Method(s);
    }
    
    public static File file(String s) {
        return new File(s);
    }
    
    private final ClassLoader classLoader;
    
    private final Module module;
    
    private final ModuleVer moduleVer;
    
    private final ClassName className;
    
    private final Method method;
    
    private final File file;
    
    private final Integer line;
    
    private final Boolean naytiv;
    
    private final int more;
    
    public CauseFrame(
        ClassLoader classLoader,
        Module module,
        ModuleVer moduleVer,
        ClassName className,
        Method method,
        File file,
        Integer line,
        boolean naytiv
    ) {
        this(
            classLoader,
            module,
            moduleVer,
            className,
            method,
            file,
            line,
            naytiv,
            -1);
    }
    
    public CauseFrame(int more) {
        this(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            more);
    }
    
    private CauseFrame(
        ClassLoader classLoader,
        Module module,
        ModuleVer moduleVer,
        ClassName className,
        Method method,
        File file,
        Integer line,
        boolean naytiv,
        int more
    ) {
        this.classLoader = classLoader;
        this.module = module;
        this.moduleVer = moduleVer;
        this.className = className;
        this.method = method;
        this.file = file;
        this.line = line == null || line < 1 ? -1 : line;
        this.naytiv = naytiv;
        this.more = more;
    }
    
    public CauseFrame unsetModuleInfo() {
        return module == null || moduleVer == null
            ? this
            : new CauseFrame(
                classLoader,
                null,
                null,
                className,
                method,
                file,
                line,
                naytiv);
    }
    
    public ClassLoader classLoader() {
        return classLoader;
    }
    
    public CauseFrame unsetClassLoader() {
        return new CauseFrame(
            null,
            module,
            moduleVer,
            className,
            method,
            file,
            line,
            naytiv);
    }
    
    public ClassName className() {
        return className;
    }
    
    public Module module() {
        return module;
    }
    
    public ModuleVer moduleVer() {
        return moduleVer;
    }
    
    public Method method() {
        return method;
    }
    
    public File file() {
        return file;
    }
    
    public int line() {
        return line;
    }
    
    public boolean naytiv() {
        return naytiv;
    }
    
    public int getMore() {
        return more;
    }
    
    public boolean isPrintable() {
        return more < 0;
    }
    
    public boolean isRef() {
        return more > 0;
    }
    
    public StackTraceElement toStackTraceElement() {
        if (isRef()) {
            throw new IllegalStateException(this + " is a reference");
        }
        return new StackTraceElement(
            StringlyTyped.toString(classLoader),
            StringlyTyped.toString(module),
            StringlyTyped.toString(moduleVer),
            StringlyTyped.toString(className),
            StringlyTyped.toString(method),
            StringlyTyped.toString(file),
            line);
    }
    
    public StringBuilder defaultPrint(StringBuilder sb) {
        if (isRef()) {
            return sb.append("... ").append(more).append(" more");
        }
        int len = sb.length();
        if (classLoader != null) {
            sb.append(classLoader).append("/");
        }
        if (module != null) {
            sb.append(module);
            if (moduleVer != null) {
                sb.append("@").append(moduleVer());
            }
        }
        if (sb.length() > len) {
            sb.append("/");
        }
        sb.append(className);
        sb.append(".").append(method).append("(");
        if (naytiv) {
            sb.append("Native Method)");
        } else if (file != null && line > 0) {
            sb.append(file).append(":").append(line).append(")");
        } else if (file != null) {
            sb.append(file).append(")");
        } else {
            sb.append("Unknown Source)");
        }
        return sb;
    }
    
    @Override
    public void hashTo(Consumer<byte[]> h) {
        if (isRef()) {
            hash(h, more);
        } else {
            hash(
                h,
                classLoader == null ? "" : classLoader.string(),
                module == null ? "" : module.string(),
                moduleVer == null ? "" : moduleVer.string(),
                className.string(),
                file.string(),
                method.string());
            hash(h, line);
            hash(h, naytiv);
        }
    }
    
    public CauseFrame shortenClassName() {
        return new CauseFrame(
            classLoader,
            module,
            moduleVer,
            className(shortened(className().string())),
            method,
            file,
            line,
            naytiv);
    }
    
    @Override
    protected StringBuilder withStringBody(StringBuilder sb) {
        return defaultPrint(sb);
    }
    
    private static String shortened(String className) {
        int dot = className.lastIndexOf(".");
        return Stream.concat(
            Arrays.stream(className.substring(0, dot).split("\\."))
                .map(part -> part.substring(0, 1)),
            Stream.of(className.substring(dot + 1))
        ).collect(Collectors.joining("."));
    }
    
    public static final class ClassLoader extends StringlyTyped {
        
        private ClassLoader(String value) {
            super(value);
        }
    }
    
    public static final class Module extends StringlyTyped {
        
        private Module(String value) {
            super(value);
        }
    }
    
    public static final class ModuleVer extends StringlyTyped {
        
        private ModuleVer(String value) {
            super(value);
        }
    }
    
    public static final class ClassName extends StringlyTyped {
        
        private ClassName(String value) {
            super(value);
        }
    }
    
    public static final class Method extends StringlyTyped {
        
        private Method(String value) {
            super(value);
        }
    }
    
    public static final class File extends StringlyTyped {
        
        private File(String value) {
            super(value);
        }
    }
}
