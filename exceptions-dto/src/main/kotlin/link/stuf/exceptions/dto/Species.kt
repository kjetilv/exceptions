package link.stuf.exceptions.dto

import java.util.*

data class Species(

        val speciesId: UUID,

        val exceptions: List<Specimen>)
