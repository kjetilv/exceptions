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

package unearth.netty;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import unearth.api.UnearthlyApi;
import unearth.api.dto.CauseIdDto;
import unearth.api.dto.CauseStrandIdDto;
import unearth.api.dto.FaultIdDto;
import unearth.api.dto.FaultStrandIdDto;
import unearth.api.dto.FeedEntryIdDto;
import unearth.norest.common.JacksonIOHandler;
import unearth.norest.common.Transformer;
import unearth.norest.netty.ApiRouter;
import unearth.norest.netty.NettyServer;
import unearth.norest.server.ApiInvoker;
import unearth.server.DefaultUnearthlyApi;
import unearth.server.Unearth;
import unearth.server.UnearthlyRenderer;

public final class Main {
    
    public static void main(String[] args) {
        new Unearth().startJavaServer((resources, configuration) -> {
            
            UnearthlyRenderer renderer = new UnearthlyRenderer(configuration.getPrefix());
            UnearthlyApi api = new DefaultUnearthlyApi(resources, renderer);
            
            ApiInvoker<UnearthlyApi> invoker =
                new ApiInvoker<>(UnearthlyApi.class, api, List.of(
                    Transformer.from(FaultIdDto.class, FaultIdDto::new),
                    Transformer.from(FaultStrandIdDto.class, FaultStrandIdDto::new),
                    Transformer.from(CauseIdDto.class, CauseIdDto::new),
                    Transformer.from(CauseStrandIdDto.class, CauseStrandIdDto::new),
                    Transformer.from(FeedEntryIdDto.class, FeedEntryIdDto::new)));
            
            ApiRouter apiRouter =
                new ApiRouter(
                    configuration.getPrefix(),
                    JacksonIOHandler.withDefaults(new ObjectMapper()),
                    invoker::response);
            NettyServer nettyServer =
                new NettyServer(configuration.getPort(), List.of(apiRouter));
            
            return new UnearthlyNettyServer(configuration, nettyServer);
        });
    }
    
    private Main() {
    }
}