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

package unearth.core;

import java.util.Optional;

import unearth.munch.id.CauseId;
import unearth.munch.id.CauseStrandId;
import unearth.munch.id.FaultId;
import unearth.munch.id.FaultStrandId;
import unearth.munch.id.FeedEntryId;
import unearth.munch.model.Cause;
import unearth.munch.model.CauseStrand;
import unearth.munch.model.Fault;
import unearth.munch.model.FaultStrand;
import unearth.munch.model.FeedEntry;
import unearth.munch.model.LogEntry;

public interface FaultStorage extends AutoCloseable, Resettable {
    
    default Runnable initStorage() {
        return () -> {
        };
    }
    
    @Override
    default void close() {
    }
    
    FeedEntry store(LogEntry logEntry, Fault fault, Throwable throwable);
    
    default Fault getRequiredFault(FaultId faultId) {
        return getFault(faultId).orElseThrow(() ->
            new IllegalArgumentException("Unknown: " + faultId));
    }
    
    Optional<Fault> getFault(FaultId faultId);
    
    default FaultStrand getRequiredFaultStrand(FaultStrandId faultStrandId) {
        return getFaultStrand(faultStrandId).orElseThrow(() ->
            new IllegalArgumentException("Unknown: " + faultStrandId));
    }
    
    Optional<FaultStrand> getFaultStrand(FaultStrandId faultStrandId);
    
    Optional<FeedEntry> getFeedEntry(FeedEntryId faultEventId);
    
    default CauseStrand getRequiredCauseStrand(CauseStrandId causeStrandId) {
        return getCauseStrand(causeStrandId).orElseThrow(() ->
            new IllegalArgumentException("Unknown: " + causeStrandId));
    }
    
    Optional<CauseStrand> getCauseStrand(CauseStrandId causeStrandId);
    
    default Cause getRequiredCause(CauseId causeId) {
        return getCause(causeId).orElseThrow(() ->
            new IllegalArgumentException("Unknown: " + causeId));
    }
    
    Optional<Cause> getCause(CauseId causeId);
}
