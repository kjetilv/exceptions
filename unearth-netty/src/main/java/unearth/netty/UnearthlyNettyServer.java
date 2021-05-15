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

import unearth.norest.netty.NettyRunner;
import unearth.server.UnearthlyConfig;
import unearth.server.UnearthlyServer;

public final class UnearthlyNettyServer implements UnearthlyServer {

    private final NettyRunner nettyServer;

    private final UnearthlyConfig config;

    public UnearthlyNettyServer(UnearthlyConfig config, NettyRunner nettyServer) {
        this.nettyServer = nettyServer;
        this.config = config;
    }

    @Override
    public void start() {
        nettyServer.start();
    }

    @Override
    public void stop() {
        nettyServer.stop();
    }

    @Override
    public void close() {
        stop();
    }

    @Override
    public int port() {
        return config.getPort();
    }
}
