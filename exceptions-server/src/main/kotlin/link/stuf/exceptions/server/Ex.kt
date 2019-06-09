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

    fun faultEventDtos(): FaultEventDto = faultEventDtos(uuid())

    fun faultEventDtos(faultTypeId: UUID): FaultEventDto {
        return FaultEventDto(uuid(),
                faultTypeId,
                random.nextLong(),
                random.nextLong(),
                random.nextLong(),
                ZonedDateTime.now(),
                wiredException())
    }

    fun faultTypeDto() = uuid().let { id ->
        FaultTypeDto(id, listOf(faultEventDtos(id)))
    }

    fun wiredException(): WiredException =
            WiredException(
                    "mymy.such.a.BadClass",
                    "Bad class!",
                    stacktrace = stack())

    fun exception(): Throwable = RuntimeException("Example throwable")

    fun exceptionOut(): String = Throwables.string(exception())

    fun stack() = CauseDto("foo.bar.BadException", emptyList(), emptyList(), uuid())
}
