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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import unearth.api.UnearthlyApi;
import unearth.api.dto.CauseIdDto;
import unearth.api.dto.CauseStrandIdDto;
import unearth.api.dto.FaultIdDto;
import unearth.api.dto.FaultStrandIdDto;
import unearth.api.dto.FeedEntryIdDto;
import unearth.norest.common.IOHandler;
import unearth.norest.common.Transformer;
import unearth.norest.server.ForwardableMethods;
import unearth.server.DefaultUnearthlyApi;
import unearth.server.Unearth;
import unearth.server.UnearthlyConfig;
import unearth.server.UnearthlyController;
import unearth.server.UnearthlyRenderer;
import unearth.server.UnearthlyServer;

public final class Main {
    
    public static void main(String[] args) {
        new Unearth().startJavaServer(Main::server);
    }
    
    private static UnearthlyServer server(UnearthlyController controller, UnearthlyConfig config) {
        ForwardableMethods<UnearthlyApi> methods = new ForwardableMethods<>(
            UnearthlyApi.class,
            List.of(
                Transformer.from(FaultIdDto.class, FaultIdDto::new),
                Transformer.from(FaultStrandIdDto.class, FaultStrandIdDto::new),
                Transformer.from(CauseIdDto.class, CauseIdDto::new),
                Transformer.from(CauseStrandIdDto.class, CauseStrandIdDto::new),
                Transformer.from(FeedEntryIdDto.class, FeedEntryIdDto::new)));
        
        UnearthlyApi api = new DefaultUnearthlyApi(
            controller,
            new UnearthlyRenderer(config.getPrefix()));
        
        UnearthlyConfig unearthlyConfig = new UnearthlyConfig();
        
        IOHandler ioHandler = new IOHandler(new ObjectMapper()
            .deactivateDefaultTyping()
            .setDateFormat(new StdDateFormat())
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
            .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, true)
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule()));
        
        return new NettyServer<>(unearthlyConfig, api, methods, ioHandler);
    }
}
