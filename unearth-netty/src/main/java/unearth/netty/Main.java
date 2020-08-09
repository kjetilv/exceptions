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

import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import unearth.api.UnearthlyApi;
import unearth.api.dto.CauseIdDto;
import unearth.api.dto.CauseStrandIdDto;
import unearth.api.dto.FaultIdDto;
import unearth.api.dto.FaultStrandIdDto;
import unearth.api.dto.FeedEntryIdDto;
import unearth.norest.common.IOHandler;
import unearth.norest.common.Transformer;
import unearth.norest.server.InvokableMethods;
import unearth.server.DefaultUnearthlyApi;
import unearth.server.Unearth;
import unearth.server.UnearthlyConfig;
import unearth.server.UnearthlyController;
import unearth.server.UnearthlyRenderer;
import unearth.server.UnearthlyServer;

public final class Main {
    
    public static void main(String[] args) {
        new Unearth().jun(Main::server);
    }
    
    private static UnearthlyServer server(UnearthlyController controller, UnearthlyConfig config) {
        return new NettyRunner<>(
            new UnearthlyConfig(),
            new DefaultUnearthlyApi(
                controller,
                new UnearthlyRenderer(config.getPrefix())),
            new InvokableMethods<>(UnearthlyApi.class, Arrays.asList(
                Transformer.from(FaultIdDto.class, FaultIdDto::new),
                Transformer.from(FaultStrandIdDto.class, FaultStrandIdDto::new),
                Transformer.from(CauseIdDto.class, CauseIdDto::new),
                Transformer.from(CauseStrandIdDto.class, CauseStrandIdDto::new),
                Transformer.from(FeedEntryIdDto.class, FeedEntryIdDto::new),
                Transformer.from(boolean.class, Boolean::parseBoolean, false),
                Transformer.from(Boolean.class, Boolean::parseBoolean),
                Transformer.from(Long.class, Long::parseLong),
                Transformer.from(long.class, Long::parseLong, 0L))),
            new IOHandler(new ObjectMapper()));
    }
}
