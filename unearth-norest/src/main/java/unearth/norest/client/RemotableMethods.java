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

package unearth.norest.client;

import java.util.HashMap;
import java.util.Map;

import unearth.norest.common.ProcessedMethod;
import unearth.norest.common.Transformers;

final class RemotableMethods {
    
    private final Transformers transformers;
    
    private final Map<java.lang.reflect.Method, RemotableMethod> callableMethods = new HashMap<>();
    
    RemotableMethods(Transformers transformers) {
        this.transformers = transformers == null ? Transformers.EMPTY : transformers;
    }
    
    RemotableMethod get(java.lang.reflect.Method method) {
        return callableMethods.computeIfAbsent(
            method,
            __ ->
                new ProcessedMethod(method, transformers));
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[metas:" + callableMethods.size() + " " + transformers + "]";
    }
}
