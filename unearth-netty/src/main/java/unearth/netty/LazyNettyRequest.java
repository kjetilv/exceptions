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
import java.util.function.Supplier;

import io.netty.handler.codec.http.FullHttpRequest;
import unearth.norest.common.Request;
import unearth.util.once.Get;

class LazyNettyRequest extends AbstractNettyRequest implements Request {
    
    private final Supplier<Method> method;
    
    private final Supplier<Map<String, Collection<String>>> headers;
    
    private final Supplier<Map<String, Collection<String>>> queryParameters;
    
    private final Supplier<Map<String, String>> singleQueryParameters;
    
    private final Supplier<Map<String, String>> singleHeaders;
    
    private final Supplier<String> entity;
    
    LazyNettyRequest(FullHttpRequest fullHttpRequest) {
        super(fullHttpRequest);
        
        this.headers = Get.mostlyOnce(this::lazyGetHeaders);
        this.method = Get.mostlyOnce(this::lazyGetMethod);
        this.queryParameters = Get.mostlyOnce(this::lazyGetQueryParameters);
        
        this.singleHeaders = Get.mostlyOnce(this::lazyGetSingleHeaders);
        this.singleQueryParameters = Get.mostlyOnce(this::lazyGetSingleQueryParameters);
        
        this.entity = Get.mostlyOnce(this::lazyGetEntity);
    }
    
    @Override
    public Method getMethod() {
        return this.method.get();
    }
    
    @Override
    public String getPath() {
        return getUri();
    }
    
    @Override
    public Map<String, Collection<String>> getQueryParameters() {
        return this.queryParameters.get();
    }
    
    @Override
    public Map<String, Collection<String>> getHeaders() {
        return this.headers.get();
    }
    
    @Override
    public Map<String, String> getSingleQueryParameters() {
        return this.singleQueryParameters.get();
    }
    
    @Override
    public Map<String, String> getSingleHeaders() {
        return singleHeaders.get();
    }
    
    @Override
    public String getEntity() {
        return entity.get();
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getMethod() + " " + getPath() + "]";
    }
}
