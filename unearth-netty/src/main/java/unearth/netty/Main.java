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
import unearth.norest.ApiInvoker;
import unearth.norest.common.JacksonIOHandler;
import unearth.norest.common.Transformer;
import unearth.norest.netty.ApiRouter;
import unearth.norest.netty.ErrorRouter;
import unearth.norest.netty.NettyServer;
import unearth.norest.server.ServerSideMethods;
import unearth.server.DefaultUnearthlyApi;
import unearth.server.Unearth;
import unearth.server.UnearthlyConfig;
import unearth.server.UnearthlyRenderer;
import unearth.server.UnearthlyResources;
import unearth.server.UnearthlyServer;

public final class Main {
    
    public static void main(String[] args) {
        new Unearth().startJavaServer(Main::server);
    }
    
    private Main() {
    }
    
    private static final List<Transformer<?>> UUID_TO_ID = List.of(
        Transformer.from(FaultIdDto.class, FaultIdDto::new),
        Transformer.from(FaultStrandIdDto.class, FaultStrandIdDto::new),
        Transformer.from(CauseIdDto.class, CauseIdDto::new),
        Transformer.from(CauseStrandIdDto.class, CauseStrandIdDto::new),
        Transformer.from(FeedEntryIdDto.class, FeedEntryIdDto::new));
    
    private static UnearthlyServer server(UnearthlyResources resources, UnearthlyConfig configuration) {
        UnearthlyRenderer renderer = new UnearthlyRenderer(configuration.getPrefix());
        UnearthlyApi api = new DefaultUnearthlyApi(resources, renderer);
        NettyServer nettyServer = nettyServer(configuration, api);
        return new UnearthlyNettyServer(configuration, nettyServer);
    }
    
    private static NettyServer nettyServer(UnearthlyConfig config, UnearthlyApi api) {
        return new NettyServer(config.getPort(), List.of(
            new ApiRouter<>(
                config.getPrefix(),
                JacksonIOHandler.withDefaults(new ObjectMapper()),
                new ApiInvoker<>(
                    api,
                    new ServerSideMethods<>(
                        UnearthlyApi.class,
                        UUID_TO_ID))),
            new ErrorRouter()));
    }
}
