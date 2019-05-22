package link.stuf.exceptions.server.api

import java.util.*

data class WiredException(

        val uuid: UUID,

        val className: String,

        val message: String?,

        val stacktrace: Array<WiredStackTraceElement>?,

        val cause: WiredException) {

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (javaClass != other?.javaClass) {
            return false
        }

        other as WiredException

        if (uuid != other.uuid) {
            return false
        }
        if (className != other.className) {
            return false
        }
        if (message != other.message) {
            return false
        }
        if (stacktrace != null) {
            if (other.stacktrace == null) {
                return false
            }
            if (!stacktrace.contentEquals(other.stacktrace)) {
                return false
            }
        } else if (other.stacktrace != null) {
            return false
        }

        return cause == other.cause
    }

    override fun hashCode(): Int {
        var result = uuid.hashCode()
        result = 31 * result + className.hashCode()
        result = 31 * result + (message?.hashCode() ?: 0)
        result = 31 * result + (stacktrace?.contentHashCode() ?: 0)
        result = 31 * result + cause.hashCode()
        return result
    }
}