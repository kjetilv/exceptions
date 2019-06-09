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
 *     along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package no.scienta.unearth.munch.data;

import java.util.function.Consumer;

public class ThrowableDto extends AbstractHashable {

    private final String message;

    private final CauseType causeType;

    private final ThrowableDto cause;

    public ThrowableDto(CauseType causeType, String message, ThrowableDto cause) {
        this.message = message;
        this.causeType = causeType;
        this.cause = cause;
    }

    public CauseType getCauseType() {
        return causeType;
    }

    public String getMessage() {
        return message;
    }

    public ThrowableDto getCause() {
        return cause;
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hashStrings(h, message);
        hashHashables(h, causeType, cause);
    }
}
