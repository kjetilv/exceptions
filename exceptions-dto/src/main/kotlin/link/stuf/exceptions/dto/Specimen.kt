package link.stuf.exceptions.dto

import java.time.ZonedDateTime
import java.util.*

data class Specimen(

        val specimenId: UUID,

        val speciesId: UUID,

        val sequence: Long,

        val time: ZonedDateTime,

        val exception: WiredException
)
