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

package unearth.netty;

import java.util.Collection;
import java.util.Map;

import io.netty.handler.codec.http.FullHttpRequest;

public class SimpleNettyRequest extends AbstractNettyRequest {
    
    SimpleNettyRequest(FullHttpRequest fullHttpRequest) {
        super(fullHttpRequest);
    }
    
    @Override
    public Method getMethod() {
        return lazyGetMethod();
    }
    
    @Override
    public String getPath() {
        return getUri();
    }
    
    @Override
    public Map<String, Collection<String>> getQueryParameters() {
        return lazyGetQueryParameters();
    }
    
    @Override
    public Map<String, Collection<String>> getHeaders() {
        return lazyGetHeaders();
    }
    
    @Override
    public String getEntity() {
        return lazyGetEntity();
    }
}
