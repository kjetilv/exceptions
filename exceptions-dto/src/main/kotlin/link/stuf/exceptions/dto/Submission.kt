package link.stuf.exceptions.dto

import java.util.*

data class Submission(

        val speciesId: UUID,

        val subspeciesId: UUID,

        val specimenId: UUID,

        val loggable: Boolean = true,

        val newSpecies: Boolean = true)
