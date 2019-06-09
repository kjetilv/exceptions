package link.stuf.exceptions.dto

import java.util.*

data class FaultDto(

        val faultTypeId: UUID,

        val faultId: UUID,

        val exceptions: List<FaultEventDto>)
