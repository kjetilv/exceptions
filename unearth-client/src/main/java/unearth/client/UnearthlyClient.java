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

import java.net.URI;
import java.util.Optional;

import unearth.api.dto.CauseDto;
import unearth.api.dto.CauseIdDto;
import unearth.api.dto.CauseStrandDto;
import unearth.api.dto.CauseStrandIdDto;
import unearth.api.dto.EventSequenceDto;
import unearth.api.dto.FaultDto;
import unearth.api.dto.FaultEventSequenceDto;
import unearth.api.dto.FaultIdDto;
import unearth.api.dto.FaultStrandDto;
import unearth.api.dto.FaultStrandEventSequenceDto;
import unearth.api.dto.FaultStrandIdDto;
import unearth.api.dto.FeedEntryDto;
import unearth.api.dto.FeedEntryIdDto;
import unearth.api.dto.Submission;

public interface UnearthlyClient {

    static UnearthlyClient connect(URI uri) {
        return new DefaultUnearthlyClient(uri);
    }

    Submission submit(String string);

    Submission submit(Throwable t);

    Optional<Throwable> throwable(FaultIdDto faultId);

    default Optional<FaultDto> fault(FaultIdDto faultIdDto) {
        return fault(faultIdDto, StackType.NONE);
    }

    Optional<FaultDto> fault(FaultIdDto faultIdDto, StackType stackType);

    default Optional<FaultStrandDto> faultStrand(FaultStrandIdDto faultIdDto) {
        return faultStrand(faultIdDto, StackType.NONE);
    }

    Optional<FaultStrandDto> faultStrand(FaultStrandIdDto faultIdDto, StackType stackType);

    default Optional<CauseDto> cause(CauseIdDto id) {
        return cause(id, StackType.NONE);
    }

    Optional<CauseDto> cause(CauseIdDto id, StackType stackType);

    default Optional<CauseStrandDto> causeStrand(CauseStrandIdDto id) {
        return causeStrand(id, StackType.NONE);
    }

    Optional<CauseStrandDto> causeStrand(CauseStrandIdDto id, StackType stackType);
    
    default Optional<FeedEntryDto> feedEntry(FeedEntryIdDto id) {
        return feedEntry(id, StackType.NONE);
    }
    
    Optional<FeedEntryDto> feedEntry(FeedEntryIdDto id, StackType stackType);

    long globalFeedMax();

    long faultFeedMax(FaultIdDto faultId);

    long faultStrandFeedMax(FaultStrandIdDto faultStrandId);

    default EventSequenceDto globalFeed() {
        return globalFeed(Page.FIRST, StackType.NONE);
    }

    EventSequenceDto globalFeed(Page page, StackType stackType);

    default EventSequenceDto globalFeed(Page page) {
        return globalFeed(page, StackType.NONE);
    }

    default FaultEventSequenceDto faultFeed(FaultIdDto faultId) {
        return faultFeed(faultId, Page.FIRST, StackType.NONE);
    }

    FaultEventSequenceDto faultFeed(FaultIdDto faultId, Page page, StackType stackType);

    default FaultEventSequenceDto faultFeed(FaultIdDto faultId, Page page) {
        return faultFeed(faultId, page, StackType.NONE);
    }

    default FaultEventSequenceDto faultFeed(FaultIdDto faultId, StackType stackType) {
        return faultFeed(faultId, Page.FIRST, stackType);
    }

    default FaultStrandEventSequenceDto faultStrandFeed(FaultStrandIdDto faultStrandId) {
        return faultStrandFeed(faultStrandId, Page.FIRST, StackType.NONE);
    }

    FaultStrandEventSequenceDto faultStrandFeed(FaultStrandIdDto faultStrandId, Page page, StackType stackType);

    default FaultStrandEventSequenceDto faultStrandFeed(FaultStrandIdDto faultStrandId, StackType stackType) {
        return faultStrandFeed(faultStrandId, Page.FIRST, stackType);
    }

    default FaultStrandEventSequenceDto faultStrandFeed(FaultStrandIdDto faultStrandId, Page page) {
        return faultStrandFeed(faultStrandId, page, StackType.NONE);
    }

    enum StackType {
        FULL,
        PRINT,
        NONE
    }
}
