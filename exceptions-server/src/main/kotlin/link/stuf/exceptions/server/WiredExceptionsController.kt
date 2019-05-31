package link.stuf.exceptions.server

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import link.stuf.exceptions.core.*
import link.stuf.exceptions.core.handler.DefaultThrowablesHandler
import link.stuf.exceptions.core.storage.InMemoryThrowablesStorage
import link.stuf.exceptions.core.throwables.ThrowableSpecies
import link.stuf.exceptions.core.throwables.ThrowableSpeciesId
import link.stuf.exceptions.core.throwables.ThrowableSpecimen
import link.stuf.exceptions.micrometer.MeteringThrowablesSensor
import link.stuf.exceptions.server.api.Occurrence
import link.stuf.exceptions.server.api.WiredException
import link.stuf.exceptions.server.api.WiredExceptions
import link.stuf.exceptions.server.api.WiredStackTraceElement
import java.time.ZoneId

class WiredExceptionsController(
        storage: ThrowablesStorage,
        feed: ThrowablesFeed,
        stats: ThrowablesStats,
        sensor: ThrowablesSensor
) {

    private val storage = InMemoryThrowablesStorage()

    private val sensor = MeteringThrowablesSensor(SimpleMeterRegistry())

    private val handler: ThrowablesHandler = DefaultThrowablesHandler(storage, feed, sensor, stats)

    fun handle(throwableInBody: Throwable?): Handling {
        return handler.handle(throwableInBody)
    }

    fun lookup(id: ThrowableSpeciesId): WiredExceptions {
        val species: ThrowableSpecies = storage.getSpecies(id)
        val specimen: List<ThrowableSpecimen> = storage.getSpecimen(id).toList()
        return WiredExceptions(species.id.hash,
                specimen.toList().map {
                    Occurrence(
                            it.id.hash,
                            it.sequence,
                            it.time.atZone(ZoneId.of("UTC")),
                            this.wiredEx(it.toThrowable()))
                })
    }

    private fun wiredEx(specimen: Throwable): WiredException = WiredException(
            className = (if (specimen is NamedException)
                (specimen as NamedException).proxiedClassName
            else
                specimen.javaClass.name),
            message = specimen.message,
            stacktrace = specimen.stackTrace.let(this::wiredStackTrace),
            cause = specimen.cause?.let(this::wiredEx))

    private fun wiredStackTrace(stackTrace: Array<StackTraceElement>): List<WiredStackTraceElement>? =
            stackTrace.map { element ->
                WiredStackTraceElement(
                        classLoaderName = element.classLoaderName,
                        moduleName = element.moduleName,
                        moduleVersion = element.moduleVersion,
                        declaringClass = element.className,
                        methodName = element.methodName,
                        fileName = element.fileName,
                        lineNumber = element.lineNumber)
            }.toList()
}
