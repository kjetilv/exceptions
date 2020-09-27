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

package unearth.api;

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
import unearth.norest.annotations.ContentType;
import unearth.norest.annotations.GET;
import unearth.norest.annotations.HEAD;
import unearth.norest.annotations.POST;
import unearth.norest.annotations.Q;

import static unearth.norest.IO.ContentType.TEXT_PLAIN;

public interface UnearthlyApi {

    @HEAD
    @ContentType(TEXT_PLAIN)
    default void pingHead() {
    }

    @GET
    @ContentType(TEXT_PLAIN)
    String ping();

    @POST("catch")
    Submission throwable(String throwable);

    @GET("fault/{}")
    Optional<FaultDto> fault(
        FaultIdDto faultId,
        @Q boolean fullStack,
        @Q boolean printStack
    );

    @GET("fault-strand/{}")
    Optional<FaultStrandDto> faultStrand(
        FaultStrandIdDto faultId,
        @Q boolean fullStack,
        @Q boolean printStack
    );

    @GET("cause/{}")
    Optional<CauseDto> cause(
        CauseIdDto causeIdDto,
        @Q boolean fullStack,
        @Q boolean printStack
    );

    @GET("cause-strand/{}")
    Optional<CauseStrandDto> causeStrand(
        CauseStrandIdDto causeStrandIdDto,
        @Q boolean fullStack,
        @Q boolean printStack
    );

    @GET("feed-entry/{}")
    Optional<FeedEntryDto> feedEntry(
        FeedEntryIdDto faultEventIdDto,
        @Q boolean fullStack,
        @Q boolean printStack
    );

    @GET("feed/limit")
    Long globalFeedLimit();

    @GET("feed/limit/fault-strand/{}")
    Long faultStrandFeedLimit(FaultStrandIdDto faultId);

    @GET("feed/limit/fault/{}")
    Long faultFeedLimit(FaultIdDto faultId);

    @GET("feed")
    EventSequenceDto globalFeed(
        @Q long offset,
        @Q long count,
        @Q boolean fullStack,
        @Q boolean printStack
    );

    @GET("feed/fault/{}")
    FaultEventSequenceDto faultFeed(
        FaultIdDto faultId,
        @Q long offset,
        @Q long count,
        @Q boolean fullStack,
        @Q boolean printStack
    );

    @GET("feed/fault-strand/{}")
    FaultStrandEventSequenceDto faultStrandFeed(
        FaultStrandIdDto faultId,
        @Q long offset,
        @Q long count,
        @Q boolean fullStack,
        @Q boolean printStack
    );
}
