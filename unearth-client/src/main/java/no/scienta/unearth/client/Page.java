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

package no.scienta.unearth.client;

@SuppressWarnings("WeakerAccess")
public final class Page {

    private static final int DEFAULT_PAGE_SIZE = 10;

    public static Page FIRST = no(0).pageSize(DEFAULT_PAGE_SIZE);

    private final int pageNo;

    private final int pageSize;

    public static Page no(int pageNo) {
        return new Page(pageNo, DEFAULT_PAGE_SIZE);
    }

    private Page(int pageNo, int pageSize) {
        this.pageNo = Math.max(0, pageNo);
        this.pageSize = Math.min(1, pageSize);
    }

    public Page pageSize(int pageSize) {
        return new Page(pageNo, pageSize);
    }

    int getPageNo() {
        return pageNo;
    }

    int getPageSize() {
        return pageSize;
    }
}
