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

package unearth.norest.common;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

public interface Request {
    
    enum Method {
        
        GET(false), HEAD(false), POST, PUT, PATCH, DELETE(false);
        
        private final boolean entity;
        
        Method() {
            this(true);
        }
        
        Method(boolean entity) {
            this.entity = entity;
        }
        
        boolean isEntity() {
            return this.entity;
        }
    }
    
    Method getMethod();
    
    String getPath();
    
    int getQueryIndex();
    
    Map<String, Collection<String>> getQueryParameters();
    
    Map<String, Collection<String>> getHeaders();
    
    List<String> getPathParameters(Matcher matcher);
    
    String getEntity();
    
    Map<String, String> getSingleQueryParameters();
    
    Map<String, String> getSingleHeaders();
}
