package link.stuf.exceptions.server.api

import java.util.*

data class WiredExceptions(
        val uuid: UUID,
        val occurrences: List<Occurrence>
)
