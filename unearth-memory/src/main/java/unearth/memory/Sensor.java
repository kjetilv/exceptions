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

package unearth.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unearth.core.FaultSensor;

public final class Sensor {
    
    private static final Logger log = LoggerFactory.getLogger(Sensor.class);
    
    public static FaultSensor memory() {
        return feedEntry ->
            log.info("{}", feedEntry);
    }
    
    private Sensor() {
    }
}
