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

package no.scienta.unearth.munch.print;

import no.scienta.unearth.munch.model.Cause;
import no.scienta.unearth.munch.model.Fault;

import java.util.Collection;

@SuppressWarnings("unused")
public interface CausesRenderer {

    default CausesRendering render(Throwable throwable) {
        return render(Fault.create(throwable));
    }

    default CausesRendering render(Fault fault) {
        return render(fault.getCauses());
    }

    CausesRendering render(Collection<Cause> causeChain);
}