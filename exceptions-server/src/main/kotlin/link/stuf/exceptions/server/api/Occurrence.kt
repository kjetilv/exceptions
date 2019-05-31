package link.stuf.exceptions.server.api

import java.time.ZonedDateTime
import java.util.*

data class Occurrence(
        val uuid: UUID,
        val sequence: Long,
        val time: ZonedDateTime,
        val exception: WiredException
)
