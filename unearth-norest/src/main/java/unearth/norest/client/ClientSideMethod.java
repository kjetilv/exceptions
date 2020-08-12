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

import java.util.Optional;

import unearth.norest.common.RequestMethod;

public interface ClientSideMethod {
    
    RequestMethod getRequestMethod();
    
    String getContentType();
    
    boolean isStringBody();
    
    Optional<Object> bodyArgument(Object... args);
    
    String buildPath(Object... args);
    
    boolean isReturnData();
    
    boolean isOptionalReturn();
    
    Class<?> getReturnType();
    
    default Object wrapResponse(Object object) {
        if (object == null) {
            if (isOptionalReturn()) {
                return Optional.empty();
            }
            throw new IllegalStateException("No object returned");
        }
        return isOptionalReturn() ? Optional.of(object) : object;
    }
}
