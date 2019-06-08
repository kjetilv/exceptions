package link.stuf.exceptions.dto

import java.util.*

data class SubSpecies(

        val speciesId: UUID,

        val specimenId: UUID,

        val exceptions: List<Specimen>)
