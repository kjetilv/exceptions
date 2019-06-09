package link.stuf.exceptions.server

import link.stuf.exceptions.dto.*
import link.stuf.exceptions.munch.Throwables
import java.time.ZonedDateTime
import java.util.*

object Ex {

    private val random = Random()

    fun uuid() = UUID.randomUUID()

    fun submission() =
            Submission(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())

    fun specimen(): Specimen = specimen(uuid())

    fun specimen(speciesId: UUID): Specimen {
        return Specimen(uuid(), speciesId, random.nextLong(), ZonedDateTime.now(), wiredException())
    }

    fun species() = uuid().let { speciesId ->
        Species(
                speciesId,
                listOf(specimen(speciesId)))
    }

    fun wiredException(): WiredException =
            WiredException(
                    "mymy.such.a.BadClass",
                    "Bad class!",
                    stacktrace = stack())

    fun exception(): Throwable = RuntimeException("Example throwable")

    fun exceptionOut(): String = Throwables.string(exception())

    fun stack() = WiredStackTrace("foo.bar.BadException", emptyList(), emptyList(), uuid())
}
