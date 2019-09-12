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

package no.scienta.unearth.munch.print;

import no.scienta.unearth.munch.base.AbstractHashable;
import no.scienta.unearth.util.StringlyTyped;

import java.util.function.Consumer;

public class CauseFrame extends AbstractHashable {

    private final ClassLoader classLoader;
    private final Module module;
    private final ModuleVer moduleVer;
    private final ClassName className;
    private final Method method;
    private final File file;
    private final Integer line;
    private final Boolean naytiv;

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
        this.classLoader = classLoader;
        this.module = module;
        this.moduleVer = moduleVer;
        this.className = className;
        this.method = method;
        this.file = file;
        this.line = line == null || line < 1 ? -1 : line;
        this.naytiv = naytiv;
    }

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

    public StackTraceElement toStackTraceElement() {
        return new StackTraceElement
            (className.getValue(), method.getValue(), file.getValue(), line);
    }

    public StringBuilder defaultPrint(StringBuilder sb) {
        int len = sb.length();
        if (isSet(classLoader)) {
            sb.append(classLoader).append("/");
        }
        if (isSet(module)) {
            sb.append(module);
            if (isSet(moduleVer)) {
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
        } else if (isSet(file) && line > 0) {
            sb.append(file).append(":").append(line).append(")");
        } else if (isSet(file)) {
            sb.append(file).append(")");
        } else {
            sb.append("Unknown Source)");
        }
        return sb;
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hash(h, classLoader.getValue(), module.getValue(), moduleVer.getValue(), className.getValue(), file.getValue());
        hashInts(h, line);
    }

    @Override
    protected String toStringBody() {
        return defaultPrint(new StringBuilder()).toString();
    }

    private static boolean isSet(StringlyTyped s) {
        return s != null;
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
