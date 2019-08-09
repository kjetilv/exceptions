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

public class CassandraInit extends AbstractCassandraConnected {

    private final String keyspace;

    public CassandraInit(String host, int port, String dc, String keyspace) {
        super(host, port, dc, null);
        this.keyspace = keyspace;
    }

    public CassandraInit init() {
        inSession(session -> {
            session.execute(
                "CREATE KEYSPACE" +
                    " IF NOT EXISTS " + keyspace +
                    " WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor' : 1}");
            session.execute(
                "USE " + keyspace);
            session.execute(
                "CREATE TABLE IF NOT EXISTS fault " +
                    "(id UUID PRIMARY KEY," +
                    " faultStrand UUID" +
                    ")"
            );
            session.execute(
                "CREATE TABLE IF NOT EXISTS faultStrand " +
                    "(id UUID PRIMARY KEY" +
                    ")"
            );
            session.execute(
                "CREATE TABLE IF NOT EXISTS faultEvent " +
                    "(id uuid PRIMARY KEY," +
                    " fault UUID," +
                    " faultStrand UUID," +
                    " globalSequenceNo BIGINT," +
                    " faultStrandSequenceNo BIGINT," +
                    " faultSequenceNo BIGINT" +
                    ")"
            );
        });
        return this;
    }
}
