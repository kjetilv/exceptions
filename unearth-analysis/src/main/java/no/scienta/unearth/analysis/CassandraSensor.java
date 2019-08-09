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

package no.scienta.unearth.analysis;

import no.scienta.unearth.core.FaultSensor;
import no.scienta.unearth.munch.id.Identifiable;
import no.scienta.unearth.munch.model.Fault;
import no.scienta.unearth.munch.model.FaultEvent;
import no.scienta.unearth.munch.model.FaultStrand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class CassandraSensor extends AbstractCassandraConnected implements FaultSensor {

    public CassandraSensor(String host, int port, String dc) {
        super(host, port, dc);
    }

    @Override
    public void register(FaultEvent faultEvent) {
        inKeyspace(session -> {
            Fault fault = faultEvent.getFault();
            FaultStrand faultStrand = fault.getFaultStrand();

            exec(session,
                "INSERT INTO fault " +
                    "(id," +
                    " faultStrand" +
                    ") VALUES (?, ?)",
                uuid(fault),
                uuid(faultStrand));
            exec(session,
                "INSERT INTO faultStrand (" +
                    "id" +
                    ") VALUES (?)",
                uuid(faultStrand));
            exec(session,
                "INSERT INTO faultEvent " +
                    "(id," +
                    " fault," +
                    " faultStrand," +
                    " globalSequenceNo," +
                    " faultSequenceNo," +
                    " faultStrandSequenceNo" +
                    ") VALUES (?, ?, ?, ?, ?, ?)",
                uuid(faultEvent),
                uuid(fault),
                uuid(faultStrand),
                faultEvent.getGlobalSequenceNo(),
                faultEvent.getFaultSequenceNo(),
                faultEvent.getFaultStrandSequenceNo()
            );
        });
    }

    private UUID uuid(Identifiable<?> identifiable) {
        return identifiable.getId().getHash();
    }

    private static final Logger log = LoggerFactory.getLogger(CassandraSensor.class);
}
