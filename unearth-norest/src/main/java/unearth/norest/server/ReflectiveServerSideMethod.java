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
package unearth.norest.server;

import java.lang.reflect.Method;
import java.util.Arrays;

import unearth.norest.Transformers;

final class ReflectiveServerSideMethod extends ServerSideMethod {

    ReflectiveServerSideMethod(Method method, Transformers transformers) {
        super(method, transformers);
    }

    @Override
    protected Object call(Object impl, Object[] args) {
        try {
            return method().invoke(impl, args);
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to invoke on " + impl + ": " + method() + "" + Arrays.toString(args), e);
        }
    }
}
