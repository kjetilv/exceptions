package link.stuf.exceptions.dto

import java.util.*

data class WiredStackTrace(
        val className: String,
        val stacktrace: List<WiredStackTraceElement>?,
        val stacktraceRef: UUID)
