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

package no.scienta.unearth.core.parser;

import java.util.regex.Pattern;
import java.util.stream.IntStream;

enum StackTraceEntry implements StackTraceElementPicker {

    NATIVE_METHOD_WITH_VERSIONED_MODULE(
        Pattern.compile("^\\s*at\\s([\\w.]*)@([\\w\\d.]*)/([\\w.]*)\\(([\\w\\s]*)\\)$")
    ) {

        @Override
        public String module(String... parts) {
            return parts[0];
        }

        @Override
        public String moduleVersion(String... parts) {
            return parts[1];
        }

        @Override
        public String className(String... parts) {
            return StackTraceEntry.getClassName(parts[2]);
        }

        @Override
        public String method(String... parts) {
            return StackTraceEntry.getMethodName(parts[2]);
        }

        @Override
        public String otherSource(String... parts) {
            return parts[3];
        }
    },

    NATIVE_METHOD_WITH_MODULE(Pattern.compile("^\\s*at\\s([\\w.]*)/([\\w.]*)\\(([\\w\\s]*)\\)$")) {

        @Override
        public String module(String... parts) {
            return parts[0];
        }

        @Override
        public String className(String... parts) {
            return StackTraceEntry.getClassName(parts[1]);
        }

        @Override
        public String method(String... parts) {
            return StackTraceEntry.getMethodName(parts[1]);
        }

        @Override
        public String otherSource(String... parts) {
            return parts[2];
        }
    },

    BASIC_NATIVE_METHOD(Pattern.compile("^\\s*at\\s([$\\w.]*)\\(([\\w\\s]*)\\)$")) {

        @Override
        public String className(String... parts) {
            return StackTraceEntry.getClassName(parts[0]);
        }

        @Override
        public String method(String... parts) {
            return StackTraceEntry.getMethodName(parts[0]);
        }

        @Override
        public String otherSource(String... parts) {
            return parts[1];
        }
    },

    WITH_VERSIONED_MODULE(
        Pattern.compile("^\\s*at\\s([\\w.]*)@([\\w\\d.]*)/([$\\w.]*)\\(([$\\w.]*):(\\d*)\\)$")
    ) {

        @Override
        public String module(String... parts) {
            return parts[0];
        }

        @Override
        public String moduleVersion(String... parts) {
            return parts[1];
        }

        @Override
        public String className(String... parts) {
            return StackTraceEntry.getClassName(parts[2]);
        }

        @Override
        public String method(String... parts) {
            return StackTraceEntry.getMethodName(parts[2]);
        }

        @Override
        public String file(String... parts) {
            return parts[3];
        }

        @Override
        public Integer lineNo(String... parts) {
            return Integer.parseInt(parts[4]);
        }
    },

    WITH_MODULE(
        Pattern.compile("^\\s*at\\s([\\w.]*)/([$\\w.]*)\\(([$\\w.]*):(\\d*)\\)$")
    ) {

        @Override
        public String module(String... parts) {
            return parts[0];
        }

        @Override
        public String className(String... parts) {
            return StackTraceEntry.getClassName(parts[1]);
        }

        @Override
        public String method(String... parts) {
            return StackTraceEntry.getMethodName(parts[1]);
        }

        @Override
        public String file(String... parts) {
            return parts[2];
        }

        @Override
        public Integer lineNo(String... parts) {
            return Integer.parseInt(parts[3]);
        }
    },

    BASIC(Pattern.compile("^\\s*at\\s([$\\w.]*)\\(([$\\w.]*):(\\d*)\\)$")) {

        @Override
        public String className(String... parts) {
            return StackTraceEntry.getClassName(parts[0]);
        }

        @Override
        public String method(String... parts) {
            return StackTraceEntry.getMethodName(parts[0]);
        }

        @Override
        public String file(String... parts) {
            return parts[1];
        }

        @Override
        public Integer lineNo(String... parts) {
            return Integer.parseInt(parts[2]);
        }
    },

    MORE(Pattern.compile("^\\s*...\\s*(\\d*)\\s*more$")) {

        @Override
        public Integer more(String... parts) {
            return Integer.parseInt(parts[0]);
        }
    };

    private static final String[] NONE = new String[0];

    private Pattern pattern;

    StackTraceEntry(Pattern pattern) {
        this.pattern = pattern;
    }

    String[] parts(String line) {
        java.util.regex.Matcher matcher = pattern.matcher(line);
        if (matcher.matches()) {
            return IntStream.range(0, matcher.groupCount())
                .map(i -> i + 1)
                .mapToObj(matcher::group)
                .toArray(String[]::new);
        }
        return NONE;
    }

    private static String getMethodName(String part) {
        int lastDot = part.lastIndexOf(".");
        return lastDot < 0 ? null : part.substring(lastDot + 1);
    }

    private static String getClassName(String part) {
        int lastDot = part.lastIndexOf(".");
        return lastDot < 0 ? null : part.substring(0, lastDot);
    }
}
