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

import unearth.client.dto.CauseDto;
import unearth.client.dto.CauseIdDto;
import unearth.client.dto.CauseStrandDto;
import unearth.client.dto.CauseStrandIdDto;
import unearth.client.dto.EventSequenceDto;
import unearth.client.dto.FaultDto;
import unearth.client.dto.FaultEventSequenceDto;
import unearth.client.dto.FaultIdDto;
import unearth.client.dto.FaultStrandDto;
import unearth.client.dto.FaultStrandEventSequenceDto;
import unearth.client.dto.FaultStrandIdDto;
import unearth.client.dto.FeedEntryDto;
import unearth.client.dto.FeedEntryIdDto;
import unearth.client.dto.Submission;

@SuppressWarnings("unused")
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

    Optional<FeedEntryDto> feedEntry(FeedEntryIdDto id);

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
