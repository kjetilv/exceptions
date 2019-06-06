package link.stuf.exceptions.dto

data class WiredException(
        val className: String,
        val message: String?,
        val stacktrace: WiredStackTrace,
        val cause: WiredException? = null)
