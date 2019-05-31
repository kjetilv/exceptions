package link.stuf.exceptions.server.api

import java.time.Instant
import java.util.*

data class Occurrence(
        val uuid: UUID,
        val sequence: Long,
        val time: Instant,
        val exception: WiredException
)
