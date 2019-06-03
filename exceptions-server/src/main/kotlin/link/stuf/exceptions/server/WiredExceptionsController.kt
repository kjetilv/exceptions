package link.stuf.exceptions.server

import link.stuf.exceptions.core.*
import link.stuf.exceptions.core.handler.DefaultThrowablesHandler
import link.stuf.exceptions.dto.*
import link.stuf.exceptions.munch.*
import java.time.ZoneId
import java.util.*

class WiredExceptionsController(
        private val storage: ThrowablesStorage,
        feed: ThrowablesFeed,
        stats: ThrowablesStats,
        sensor: ThrowablesSensor
) {
    private val handler: ThrowablesHandler = DefaultThrowablesHandler(storage, feed, sensor, stats)

    fun handle(throwableInBody: Throwable?): HandlingPolicy =
            handler.handle(throwableInBody)

    fun lookupSpecies(id: ThrowableSpeciesId, fullStack: Boolean = false): SpeciesExceptions {
        val speciesId = storage.resolve(id.hash)
        val specimen = storage.getSpecimensOf(speciesId)
        val species = storage.getSpecies(speciesId)
        return SpeciesExceptions(
                species.id.hash,
                specimen.toList().map { specimen ->
                    Specimen(
                            specimen.id.hash,
                            species.id.hash,
                            specimen.typeSequence,
                            specimen.time.atZone(ZoneId.of("UTC")),
                            wiredException(specimen.toThrowableDto(), fullStack)
                    )
                }
        )
    }

    fun lookupSpecimen(id: ThrowableSpecimenId, fullStack: Boolean = false) =
            storage.getSpecimen(id).let { specimen ->
                Specimen(
                        specimen.id.hash,
                        specimen.species.id.hash,
                        specimen.typeSequence,
                        specimen.time.atZone(ZoneId.of("UTC")),
                        wiredException(specimen.toThrowableDto(), fullStack))
            }

    fun lookupStack(stackId: ThrowableStackId, fullStack: Boolean = false): WiredStackTrace =
            wiredStack(storage.getStack(stackId), stackId.hash, fullStack)

    fun lookupPrintable(specimenId: ThrowableSpecimenId): String =
            Throwables.string(storage.getSpecimen(specimenId).toThrowable())

    private fun wiredStack(stack: ThrowableStack, stacktraceRef: UUID, fullStack: Boolean = false): WiredStackTrace =
            WiredStackTrace(
                    stack.className,
                    if (fullStack) wiredStackTrace(stack.stackTrace) else emptyList(),
                    stacktraceRef)

    private fun wiredException(specimen: ThrowableDto, fullStack: Boolean = false): WiredException = WiredException(
            className = specimen.className,
            message = specimen.message,
            stacktrace = wiredStack(specimen.stack, specimen.stack.hash, fullStack),
            cause = specimen.cause?.let { cause -> wiredException(cause, fullStack) })

    private fun wiredStackTrace(stackTrace: List<StackTraceElement>): List<WiredStackTraceElement>? =
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
