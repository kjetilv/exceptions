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

import no.scienta.unearth.client.dto.*;

import java.net.URI;

@SuppressWarnings("unused")
public interface UnearthlyClient {

    static UnearthlyClient connect(URI uri) {
        return new DefaultUnearthlyClient(uri);
    }

    Submission submit(String string);

    Submission submit(Throwable t);

    Throwable throwable(FaultIdDto faultId);

    default FaultDto fault(FaultIdDto faultIdDto) {
        return fault(faultIdDto, StackType.NONE);
    }

    FaultDto fault(FaultIdDto faultIdDto, StackType stackType);

    default FaultStrandDto faultStrand(FaultStrandIdDto faultIdDto) {
        return faultStrand(faultIdDto, StackType.NONE);
    }

    FaultStrandDto faultStrand(FaultStrandIdDto faultIdDto, StackType stackType);

    default CauseDto cause(CauseIdDto id) {
        return cause(id, StackType.NONE);
    }

    CauseDto cause(CauseIdDto id, StackType stackType);

    default CauseStrandDto causeStrand(CauseStrandIdDto id) {
        return causeStrand(id, StackType.NONE);
    }

    CauseStrandDto causeStrand(CauseStrandIdDto id, StackType stackType);

    FaultEventDto faultEvent(FaultEventIdDto faultEventId);

    long globalFeedMax();

    long faultFeedMax(FaultIdDto faultId);

    long faultStrandFeedMax(FaultStrandIdDto faultStrandId);

    default EventSequenceDto globalFeed() {
        return globalFeed(Page.FIRST, StackType.NONE);
    }

    default EventSequenceDto globalFeed(Page page) {
        return globalFeed(page, StackType.NONE);
    }

    EventSequenceDto globalFeed(Page page, StackType stackType);

    default FaultEventSequenceDto faultFeed(FaultIdDto faultId) {
        return faultFeed(faultId, Page.FIRST, StackType.NONE);
    }

    default FaultEventSequenceDto faultFeed(FaultIdDto faultId, Page page) {
        return faultFeed(faultId, page, StackType.NONE);
    }

    default FaultEventSequenceDto faultFeed(FaultIdDto faultId, StackType stackType) {
        return faultFeed(faultId, Page.FIRST, stackType);
    }

    FaultEventSequenceDto faultFeed(FaultIdDto faultId, Page page, StackType stackType);

    default FaultStrandEventSequenceDto faultStrandFeed(FaultStrandIdDto faultStrandId) {
        return faultStrandFeed(faultStrandId, Page.FIRST, StackType.NONE);
    }

    default FaultStrandEventSequenceDto faultStrandFeed(FaultStrandIdDto faultStrandId, StackType stackType) {
        return faultStrandFeed(faultStrandId, Page.FIRST, stackType);
    }

    default FaultStrandEventSequenceDto faultStrandFeed(FaultStrandIdDto faultStrandId, Page page) {
        return faultStrandFeed(faultStrandId, page, StackType.NONE);
    }

    FaultStrandEventSequenceDto faultStrandFeed(FaultStrandIdDto faultStrandId, Page page, StackType stackType);

    enum StackType {

        FULL,

        PRINT,

        NONE
    }
}
