package link.stuf.exceptions.server.api

data class WiredException(
        val className: String,
        val message: String?,
        val stacktrace: List<WiredStackTraceElement>?,
        val cause: WiredException?)
