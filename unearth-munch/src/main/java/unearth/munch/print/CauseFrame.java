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

import java.util.function.Consumer;

import unearth.munch.base.AbstractHashable;
import unearth.util.StringlyTyped;

public final class CauseFrame extends AbstractHashable {
    
    public static ClassLoader ClassLoader(String s) {
        return new ClassLoader(s);
    }
    
    public static Module Module(String s) {
        return new Module(s);
    }
    
    public static ModuleVer ModuleVer(String s) {
        return new ModuleVer(s);
    }
    
    public static ClassName ClassName(String s) {
        return new ClassName(s);
    }
    
    public static Method Method(String s) {
        return new Method(s);
    }
    
    public static File File(String s) {
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
        this(classLoader, module, moduleVer, className, method, file, line, naytiv, -1);
    }
    
    public CauseFrame(int more) {
        this(null, null, null, null, null, null, null, false, more);
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
        return module != null && moduleVer != null
            ? new CauseFrame(classLoader, null, null, className, method, file, line, naytiv)
            : this;
    }
    
    public ClassLoader classLoader() {
        return classLoader;
    }
    
    public CauseFrame unsetClassLoader() {
        return classLoader(null);
    }
    
    public CauseFrame classLoader(ClassLoader classLoader) {
        return new CauseFrame(classLoader, module, moduleVer, className, method, file, line, naytiv);
    }
    
    public ClassName className() {
        return className;
    }
    
    public CauseFrame className(ClassName className) {
        return new CauseFrame(classLoader, module, moduleVer, className, method, file, line, naytiv);
    }
    
    public Module module() {
        return module;
    }
    
    public CauseFrame module(Module module) {
        return new CauseFrame(classLoader, module, moduleVer, className, method, file, line, naytiv);
    }
    
    public ModuleVer moduleVer() {
        return moduleVer;
    }
    
    public CauseFrame moduleVer(ModuleVer moduleVer) {
        return new CauseFrame(classLoader, module, moduleVer, className, method, file, line, naytiv);
    }
    
    public Method method() {
        return method;
    }
    
    public CauseFrame method(Method method) {
        return new CauseFrame(classLoader, module, moduleVer, className, method, file, line, naytiv);
    }
    
    public File file() {
        return file;
    }
    
    public CauseFrame file(File file) {
        return new CauseFrame(classLoader, module, moduleVer, className, method, file, line, naytiv);
    }
    
    public int line() {
        return line;
    }
    
    public CauseFrame line(Integer line) {
        return new CauseFrame(classLoader, module, moduleVer, className, method, file, line, naytiv);
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
                classLoader == null ? "" : classLoader.stringValue(),
                module == null ? "" : module.stringValue(),
                moduleVer == null ? "" : moduleVer.stringValue(),
                className.stringValue(),
                file.stringValue(),
                method.stringValue());
            hashInts(h, line);
            hashBools(h, naytiv);
        }
    }
    
    @Override
    protected String toStringBody() {
        return defaultPrint(new StringBuilder()).toString();
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
