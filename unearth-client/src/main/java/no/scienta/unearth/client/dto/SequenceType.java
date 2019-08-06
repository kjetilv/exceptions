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

package no.scienta.unearth.client.dto;

import java.util.function.Function;

public enum SequenceType implements Function<FaultEventDto, Long>{

    GLOBAL(dto -> dto.sequenceNo),

    FAULT_STRAND(dto -> dto.faultStrandSequenceNo),

    FAULT(dto -> dto.faultSequenceNo);

    private final Function<FaultEventDto, Long> seqNo;

    SequenceType(Function<FaultEventDto, Long> seqNo) {
        this.seqNo = seqNo;
    }

    @Override
    public Long apply(FaultEventDto faultEventDto) {
        return seqNo.apply(faultEventDto);
    }
}
