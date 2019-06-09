package link.stuf.exceptions.dto

import java.util.*

data class FaultTypeDto(

        val faultTypeId: UUID,

        val exceptions: List<FaultEventDto>)
