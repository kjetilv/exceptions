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

package no.scienta.unearth.munch.model;

import no.scienta.unearth.munch.base.AbstractHashableIdentifiable;
import no.scienta.unearth.munch.id.FaultLogId;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class LogEntry extends AbstractHashableIdentifiable<FaultLogId> {

    private final String logMessage;
    private final String[] args;

    private LogEntry(
        String logMessage,
        Object... args
    ) {
        this.logMessage = Objects.requireNonNull(logMessage, "logMessage");
        this.args = Optional.ofNullable(args).stream()
            .flatMap(Arrays::stream)
            .map(String::valueOf)
            .toArray(String[]::new);
        if (this.logMessage.isBlank()) {
            throw new IllegalArgumentException("Empty log statement");
        }
    }

    public static LogEntry create(String logMessage, Object... args) {
        return new LogEntry(logMessage, args);
    }

    public String getLogMessage() {
        return logMessage;
    }

    public String[] getArgs() {
        return args;
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hash(h, logMessage);
        hash(h, args);
    }

    @Override
    protected FaultLogId id(UUID hash) {
        return new FaultLogId(hash);
    }
}
