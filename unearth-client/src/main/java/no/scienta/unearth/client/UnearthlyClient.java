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

package no.scienta.unearth.client;

import no.scienta.unearth.dto.*;

import java.net.URI;
import java.nio.file.Path;

public interface UnearthlyClient {

    static UnearthlyClient connect(URI uri) {
        return new DefaultUnearthlyClient(uri);
    }

    Submission submit(Path path);

    Submission submit(String string);

    Submission submit(Throwable t);

    Throwable throwable(FaultIdDto faultId);

    default FaultDto fault(FaultIdDto faultIdDto) {
        return fault(faultIdDto, StackType.NONE);
    }

    FaultDto fault(FaultIdDto faultIdDto, StackType stackType);

    default FaultStrandDto faultStrand(FaultStrandIdDto faultIdDto) {
        return faultStrand(faultIdDto, StackType.NONE);
    }

    FaultStrandDto faultStrand(FaultStrandIdDto faultIdDto, StackType stackType);

    default CauseDto cause(CauseIdDto id) {
        return cause(id, StackType.NONE);
    }

    CauseDto cause(CauseIdDto id, StackType stackType);

    default CauseStrandDto causeStrand(CauseStrandIdDto id) {
        return causeStrand(id, StackType.NONE);
    }

    CauseStrandDto causeStrand(CauseStrandIdDto id, StackType stackType);

    FaultEventDto faultEvent(FaultEventIdDto faultEventId);

    enum StackType {

        FULL,

        PRINT,

        NONE
    }
}
