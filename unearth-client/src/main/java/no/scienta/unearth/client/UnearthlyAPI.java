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
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface UnearthlyAPI {

    @POST("catch")
    Call<Submission> throwable(@Body Throwable throwable);

    @POST("catch")
    Call<Submission> throwable(@Body RequestBody throwable);

    @GET("fault/{uuid}")
    Call<FaultDto> fault(@Path("uuid") FaultIdDto faultId,
                         @Query("fullStack") boolean fullStack,
                         @Query("printStack") boolean printStack);

    @GET("fault-strand/{uuid}")
    Call<FaultStrandDto> faultStrand(@Path("uuid") FaultStrandIdDto faultId,
                                     @Query("fullStack") boolean fullStack,
                                     @Query("printStack") boolean printStack);

    @GET("cause/{uuid}")
    Call<CauseDto> cause(@Path("uuid") CauseIdDto causeIdDto,
                         @Query("fullStack") boolean fullStack,
                         @Query("printStack") boolean printStack);

    @GET("cause-strand/{uuid}")
    Call<CauseStrandDto> causeStrand(@Path("uuid") CauseStrandIdDto causeIdDto,
                                     @Query("fullStack") boolean fullStack,
                                     @Query("printStack") boolean printStack);

    @GET("fault-event/{uuid}")
    Call<FaultEventDto> faultEvent(@Path("uuid") FaultEventIdDto faultEventIdDto);
}
