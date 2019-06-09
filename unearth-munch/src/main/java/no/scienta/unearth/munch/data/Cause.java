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

import no.scienta.unearth.munch.ids.CauseId;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class Cause extends AbstractHashableIdentifiable<CauseId> {

    private final CauseType causeType;

    private final String message;

    static Cause create(Throwable cause) {
        return new Cause(CauseType.create(cause), cause.getMessage());
    }

    private Cause(CauseType causeType, String message) {
        this.causeType = Objects.requireNonNull(causeType);
        this.message = message;
    }

    public CauseType getCauseType() {
        return causeType;
    }

    public String getMessage() {
        return message;
    }

    Throwable toThrowable(Throwable t) {
        Throwable exception = new ChameleonException(causeType.getClassName(), message, t);
        exception.setStackTrace(causeType.getStackTrace().toArray(StackTraceElement[]::new));
        return exception;
    }

    ThrowableDto toThrowableDto(ThrowableDto cause) {
        return new ThrowableDto(this.causeType, this.message, cause);
    }

    @Override
    protected CauseId id(UUID hash) {
        return new CauseId(hash);
    }

    @Override
    protected String toStringBody() {
        return "causeType:" + causeType + " message:" + message;
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hashHashables(h, causeType);
        hashString(h, message);
    }
}
