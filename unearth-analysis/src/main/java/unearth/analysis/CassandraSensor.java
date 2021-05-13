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
package unearth.analysis;

import java.util.UUID;

import unearth.core.FaultSensor;
import unearth.hashable.Hashed;
import unearth.munch.model.FeedEntry;

public class CassandraSensor extends AbstractCassandraConnected implements FaultSensor {

    public CassandraSensor(String host, int port, String dc, String keyspace) {
        super(host, port, dc, keyspace);
    }

    @Override
    public void register(FeedEntry feedEntry) {
        inSession(session -> {
            exec(
                session,
                "INSERT INTO fault " +
                "(id," +
                " faultStrand" +
                ") VALUES (?, ?)",
                uuid(feedEntry.getFaultEvent().getFaultId()),
                uuid(feedEntry.getFaultEvent().getFaultStrandId()));
            exec(
                session,
                "INSERT INTO faultStrand (" +
                "id" +
                ") VALUES (?)",
                uuid(feedEntry.getFaultEvent().getFaultStrandId()));
            exec(
                session,
                "INSERT INTO faultEvent " +
                "(id," +
                " fault," +
                " faultStrand," +
                " globalSequenceNo," +
                " faultSequenceNo," +
                " faultStrandSequenceNo" +
                ") VALUES (?, ?, ?, ?, ?, ?)",
                uuid(feedEntry.getFaultEvent()),
                uuid(feedEntry.getFaultEvent().getFaultId()),
                uuid(feedEntry.getFaultEvent().getFaultStrandId()),
                feedEntry.getGlobalSequenceNo(),
                feedEntry.getFaultSequenceNo(),
                feedEntry.getFaultStrandSequenceNo()
            );
        });
    }

    private static UUID uuid(Hashed identifiable) {
        return identifiable.getHash();
    }
}
