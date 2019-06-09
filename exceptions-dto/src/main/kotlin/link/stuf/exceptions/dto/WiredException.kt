package link.stuf.exceptions.dto

data class WiredException(

        val className: String,

        val message: String?,

        val stacktrace: CauseDto,

        val cause: WiredException? = null)
