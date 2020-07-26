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

import java.util.Optional;

import no.scienta.unearth.client.dto.CauseDto;
import no.scienta.unearth.client.dto.CauseIdDto;
import no.scienta.unearth.client.dto.CauseStrandDto;
import no.scienta.unearth.client.dto.CauseStrandIdDto;
import no.scienta.unearth.client.dto.EventSequenceDto;
import no.scienta.unearth.client.dto.FaultDto;
import no.scienta.unearth.client.dto.FaultEventSequenceDto;
import no.scienta.unearth.client.dto.FaultIdDto;
import no.scienta.unearth.client.dto.FaultStrandDto;
import no.scienta.unearth.client.dto.FaultStrandEventSequenceDto;
import no.scienta.unearth.client.dto.FaultStrandIdDto;
import no.scienta.unearth.client.dto.FeedEntryDto;
import no.scienta.unearth.client.dto.FeedEntryIdDto;
import no.scienta.unearth.client.dto.Submission;
import no.scienta.unearth.client.proto.GET;
import no.scienta.unearth.client.proto.POST;
import no.scienta.unearth.client.proto.Q;

public interface UnearthlyAPI {

    @POST("catch")
    Submission throwable(String throwable);

    @GET("fault/{}")
    Optional<FaultDto> fault(FaultIdDto faultId, @Q Boolean fullStack, @Q Boolean printStack);

    @GET("fault-strand/{}")
    Optional<FaultStrandDto> faultStrand(FaultStrandIdDto faultId, @Q Boolean fullStack, @Q Boolean printStack);

    @GET("cause/{}")
    Optional<CauseDto> cause(CauseIdDto causeIdDto, @Q Boolean fullStack, @Q Boolean printStack);

    @GET("cause-strand/{}")
    Optional<CauseStrandDto> causeStrand(CauseStrandIdDto causeIdDto, @Q Boolean fullStack, @Q Boolean printStack);

    @GET("feed-entry/{}")
    Optional<FeedEntryDto> feedEntry(FeedEntryIdDto faultEventIdDto);

    @GET("feed/limit")
    Long globalFeedLimit();

    @GET("feed/limit/fault-strand/{}")
    Long faultStrandFeedLimit(FaultStrandIdDto faultId);

    @GET("feed/limit/fault/{}")
    Long faultFeedLimit(FaultIdDto faultId);

    @GET("feed")
    EventSequenceDto globalFeed(
        @Q Integer offset,
        @Q Integer count,
        @Q Boolean fullStack,
        @Q Boolean printStack
    );

    @GET("feed/fault/{}")
    FaultEventSequenceDto faultFeed(
        FaultIdDto faultId,
        @Q Integer offset,
        @Q Integer count,
        @Q Boolean fullStack,
        @Q Boolean printStack
    );

    @GET("feed/fault-strand/{}")
    FaultStrandEventSequenceDto faultStrandFeed(
        FaultStrandIdDto faultId,
        @Q Integer offset,
        @Q Integer count,
        @Q Boolean fullStack,
        @Q Boolean printStack
    );
}
