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

package no.scienta.unearth.core;

import no.scienta.unearth.munch.id.FaultEventId;
import no.scienta.unearth.munch.id.FaultId;
import no.scienta.unearth.munch.id.FaultStrandId;
import no.scienta.unearth.munch.model.Fault;
import no.scienta.unearth.munch.model.FeedEntry;

public interface HandlingPolicy {

    String getSummary();

    Action getAction();

    FaultStrandId getFaultStrandId();

    FaultId getFaultId();

    FaultEventId getFaultEventId();

    FeedEntry getFeedEntry();

    Fault getFault();

    long getGlobalSequence();

    long getFaultStrandSequence();

    long getFaultSequence();

    enum Action {
        LOG,
        LOG_SHORT,
        LOG_MESSAGES,
        LOG_ID
    }
}
