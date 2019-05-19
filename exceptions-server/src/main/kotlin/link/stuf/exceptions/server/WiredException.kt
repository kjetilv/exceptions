package link.stuf.exceptions.server

import java.util.*

data class WiredException(
        val uuid: UUID,
        val className: String,
        val message: String?,
        val stacktrace: WiredStackTrace?,
        val cause: WiredException)
