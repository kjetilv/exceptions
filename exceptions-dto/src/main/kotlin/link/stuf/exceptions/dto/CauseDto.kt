package link.stuf.exceptions.dto

import java.util.*

data class CauseDto(

        val className: String,

        val stacktrace: List<WiredStackTraceElement>?,

        val simpleTrace: List<String>,

        val stacktraceRef: UUID)
