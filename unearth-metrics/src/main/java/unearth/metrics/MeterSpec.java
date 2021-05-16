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

package unearth.metrics;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;

record MeterSpec(Method method, Object[] args) {

    Collection<Tag> tags() {
        String[] parameters = parameters();
        return IntStream.range(0, parameters.length)
            .filter(i -> args[i] != null)
            .mapToObj(i -> Tag.of(parameters[i], string(args[i])))
            .collect(Collectors.toSet());
    }

    Class<? extends Meter> returnType() {
        return method.getReturnType().asSubclass(Meter.class);
    }

    private String[] parameters() {
        return Arrays.stream(method.getParameters())
            .map(Parameter::getName)
            .toArray(String[]::new);
    }

    private static String string(Object arg) {
        if (arg instanceof Class<?>) {
            return ((Class<?>) arg).getName();
        }
        if (arg instanceof String) {
            return (String) arg;
        }
        return String.valueOf(arg);
    }
}
