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

package no.scienta.unearth.core.handler;

import no.scienta.unearth.core.HandlingPolicy;
import no.scienta.unearth.munch.id.FaultId;
import no.scienta.unearth.munch.id.FaultStrandId;
import no.scienta.unearth.munch.id.FeedEntryId;
import no.scienta.unearth.munch.model.Fault;
import no.scienta.unearth.munch.model.FeedEntry;

class SimpleHandlingPolicy implements HandlingPolicy {

    private final String summary;

    private final FeedEntry feedEntry;

    private final Fault fault;

    private final Action action;

    SimpleHandlingPolicy(FeedEntry feedEntry, Fault fault) {
        this(null, feedEntry, fault, null);
    }

    private SimpleHandlingPolicy(
        String summary,
        FeedEntry feedEntry,
        Fault fault,
        Action action
    ) {
        this.summary = summary;
        this.feedEntry = feedEntry;
        this.fault = fault;
        this.action = action;
    }

    @Override
    public String getSummary() {
        return summary;
    }

    @Override
    public Action getAction() {
        return action;
    }

    @Override
    public FaultStrandId getFaultStrandId() {
        return feedEntry.getFaultEvent().getFaultStrandId();
    }

    @Override
    public FaultId getFaultId() {
        return feedEntry.getFaultEvent().getFaultId();
    }

    @Override
    public FeedEntryId getFeedEntryId() {
        return feedEntry.getId();
    }

    @Override
    public FeedEntry getFeedEntry() {
        return feedEntry;
    }

    @Override
    public Fault getFault() {
        return fault;
    }

    @Override
    public long getGlobalSequence() {
        return feedEntry.getGlobalSequenceNo();
    }

    @Override
    public long getFaultStrandSequence() {
        return feedEntry.getFaultStrandSequenceNo();
    }

    @Override
    public long getFaultSequence() {
        return feedEntry.getFaultSequenceNo();
    }

    SimpleHandlingPolicy withSummary(String summary) {
        return new SimpleHandlingPolicy(summary, feedEntry, fault, action);
    }

    SimpleHandlingPolicy withAction(Action action) {
        return new SimpleHandlingPolicy(summary, feedEntry, fault, action);
    }
}
