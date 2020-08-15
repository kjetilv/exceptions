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

package unearth.norest.netty;

import java.util.function.Consumer;

import unearth.hashable.AbstractHashable;
import unearth.norest.common.Request;
import unearth.norest.common.Response;

public class SimpleResponse extends AbstractHashable implements Response {
    
    private final Request request;
    
    private final byte[] bytes;
    
    public SimpleResponse(Request request, byte[] bytes) {
        this.request = request;
        this.bytes = bytes == null || bytes.length == 0 ? NO_BYTES : bytes;
    }
    
    @Override
    public Request getRequest() {
        return request;
    }
    
    @Override
    public byte[] getEntity() {
        return bytes;
    }
    
    @Override
    public void hashTo(Consumer<byte[]> h) {
        hash(h, request);
        hashThis(h);
    }
    
    @Override
    protected Object toStringBody() {
        return bytes.length;
    }
    
    private static final byte[] NO_BYTES = new byte[0];
}
