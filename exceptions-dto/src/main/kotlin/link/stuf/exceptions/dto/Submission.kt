package link.stuf.exceptions.dto

import java.util.*

data class Submission(

        val faultTypeId: UUID,

        val faultId: UUID,

        val faultEventId: UUID,

        val loggable: Boolean = true,

        val newType: Boolean = true)
