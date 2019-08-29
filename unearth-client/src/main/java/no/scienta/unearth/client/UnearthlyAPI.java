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
import no.scienta.unearth.client.proto.GET;
import no.scienta.unearth.client.proto.POST;
import no.scienta.unearth.client.proto.Q;

import java.util.Optional;

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

    @GET("fault-event/{}")
    Optional<FaultEventDto> faultEvent(FaultEventIdDto faultEventIdDto);

    @GET("feed/limit")
    Long globalFeedLimit();

    @GET("feed/limit/fault-strand/{}")
    Long faultStrandFeedLimit(FaultStrandIdDto faultId);

    @GET("feed/limit/fault/{}")
    Long faultFeedLimit(FaultIdDto faultId);

    @GET("feed")
    EventSequenceDto globalFeed(@Q Integer offset,
                                @Q Integer count,
                                @Q Boolean fullStack,
                                @Q Boolean printStack);

    @GET("feed/fault/{}")
    FaultEventSequenceDto faultFeed(FaultIdDto faultId,
                                    @Q Integer offset,
                                    @Q Integer count,
                                    @Q Boolean fullStack,
                                    @Q Boolean printStack);

    @GET("feed/fault-strand/{}")
    FaultStrandEventSequenceDto faultStrandFeed(FaultStrandIdDto faultId,
                                                @Q Integer offset,
                                                @Q Integer count,
                                                @Q Boolean fullStack,
                                                @Q Boolean printStack);
}
