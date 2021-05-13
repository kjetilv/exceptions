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

import java.util.Arrays;
import java.util.Objects;

import unearth.norest.common.Request;
import unearth.norest.common.Response;

public class SimpleResponse implements Response {

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

    private static final byte[] NO_BYTES = new byte[0];

    @Override
    public int hashCode() {
        return 31 * Objects.hash(request) + Arrays.hashCode(bytes);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof SimpleResponse &&
                            Objects.equals(request, ((SimpleResponse) o).request) &&
                            Arrays.equals(bytes, ((SimpleResponse) o).bytes);
    }
}
