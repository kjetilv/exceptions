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

package unearth.client;

import java.util.Objects;
import java.util.Optional;

public class ChameleonException extends Exception {

    private final String proxiedClassName;

    ChameleonException(String proxiedClassName, String message, Throwable cause) {
        super(message, cause, true, true);
        this.proxiedClassName = Objects.requireNonNull(proxiedClassName, "proxiedClassName");
    }

    @Override
    public String toString() {
        return proxiedClassName + Optional.ofNullable(getMessage()).map(msg -> ": " + msg).orElse("");
    }
}
