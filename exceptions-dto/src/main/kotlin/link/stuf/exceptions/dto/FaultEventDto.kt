package link.stuf.exceptions.dto

import java.time.ZonedDateTime
import java.util.*

data class FaultEventDto(

        val faultId: UUID,

        val faultTypeId: UUID,

        val sequence: Long,

        val faultSequence: Long,

        val faultTypeSequence: Long,

        val time: ZonedDateTime,

        val exception: WiredException
)
