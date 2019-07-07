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

import no.scienta.unearth.munch.base.AbstractHashable;

import java.util.function.Consumer;

public class CauseFrame extends AbstractHashable {

    private final String classLoader;

    private final String module;

    private final String moduleVer;

    private final String className;

    private final String method;

    private final String file;

    private final int line;

    private final boolean naytiv;

    public CauseFrame(
        String classLoader,
        String module,
        String moduleVer,
        String className,
        String method,
        String file,
        Integer line,
        boolean naytiv
    ) {
        this.classLoader = norm(classLoader);
        this.module = norm(module);
        this.moduleVer = norm(moduleVer);
        this.className = norm(className);
        this.method = norm(method);
        this.file = norm(file);
        this.line = line == null || line < 1 ? -1 : line;
        this.naytiv = naytiv;
    }

    public CauseFrame unsetModuleInfo() {
        return module != null && moduleVer != null
            ? new CauseFrame(classLoader, null, null, className, method, file, line, naytiv)
            : this;
    }

    public String classLoader() {
        return classLoader;
    }

    public CauseFrame unsetClassLoader() {
        return classLoader(null);
    }

    public CauseFrame classLoader(String classLoader) {
        return new CauseFrame(classLoader, module, moduleVer, className, method, file, line, naytiv);
    }

    public String className() {
        return className;
    }

    public CauseFrame className(String className) {
        return new CauseFrame(classLoader, module, moduleVer, className, method, file, line, naytiv);
    }

    public String module() {
        return module;
    }

    public CauseFrame module(String module) {
        return new CauseFrame(classLoader, module, moduleVer, className, method, file, line, naytiv);
    }

    public String moduleVer() {
        return moduleVer;
    }

    public CauseFrame moduleVer(String moduleVer) {
        return new CauseFrame(classLoader, module, moduleVer, className, method, file, line, naytiv);
    }

    public String method() {
        return method;
    }

    public CauseFrame method(String method) {
        return new CauseFrame(classLoader, module, moduleVer, className, method, file, line, naytiv);
    }

    public String file() {
        return file;
    }

    public CauseFrame file(String file) {
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
        return new StackTraceElement(className, method, file, line);
    }

    private static String norm(String s) {
        return blank(s) ? "" : s;
    }

    private static boolean isSet(String s) {
        return !blank(s);
    }

    private static boolean blank(String s) {
        return s == null || s.length() == 0 || s.trim().isEmpty();
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
    protected String toStringBody() {
        return defaultPrint(new StringBuilder()).toString();
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hash(h, classLoader, module, moduleVer, className, file);
        hash(h, line);
    }
}
