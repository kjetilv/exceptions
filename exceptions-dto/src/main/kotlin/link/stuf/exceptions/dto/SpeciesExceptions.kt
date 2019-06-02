package link.stuf.exceptions.dto

import java.util.*

data class SpeciesExceptions(val speciesId: UUID, val exceptions: List<Specimen>)
