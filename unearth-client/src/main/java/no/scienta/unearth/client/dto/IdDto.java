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

package no.scienta.unearth.client.dto;

import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"unused", "WeakerAccess"})
public class IdDto {

    public UUID uuid;
    public String type;
    public String link;

    public IdDto() {
        this(null, null);
    }

    IdDto(String type) {
        this(type, null);
    }

    IdDto(String type, UUID uuid) {
        this.type = type;
        this.uuid = uuid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, link);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass() &&
            Objects.equals(uuid, ((IdDto) o).uuid) &&
            Objects.equals(type, ((IdDto) o).type);
    }

    @Override
    public String toString() {
        return type + ":" + uuid;
    }
}
