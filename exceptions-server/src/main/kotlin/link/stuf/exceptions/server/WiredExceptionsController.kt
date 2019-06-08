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

    fun lookupSpecies(id: ThrowableSpeciesId, fullStack: Boolean = false): Species {
        val speciesId = storage.resolve(id.hash)
        val specimens = storage.getSpecimensOf(speciesId)
        val species = storage.getSpecies(speciesId)
        return Species(
                species.id.hash,
                specimens.toList().map { specimen ->
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

    fun lookupSpecimen(
            id: ThrowableSpecimenId,
            fullStack: Boolean = false,
            simpleTrace: Boolean = false
    ): Specimen =
            storage.getSpecimen(id).let { specimen ->
                Specimen(
                        specimen.id.hash,
                        specimen.species.id.hash,
                        specimen.typeSequence,
                        specimen.time.atZone(ZoneId.of("UTC")),
                        wiredException(specimen.toThrowableDto(), fullStack, simpleTrace))
            }

    fun lookupStack(
            stackId: ThrowableStackId,
            fullStack: Boolean = false,
            simpleTrace: Boolean = false
    ): WiredStackTrace =
            wiredStack(storage.getStack(stackId), stackId.hash, fullStack, simpleTrace)

    fun lookupPrintable(specimenId: ThrowableSpecimenId): String =
            Throwables.string(lookupThrowable(specimenId))

    fun lookupThrowable(specimenId: ThrowableSpecimenId): Throwable =
            storage.getSpecimen(specimenId).toThrowable()

    private fun wiredException(
            specimen: ThrowableDto,
            fullStack: Boolean = false,
            simpleTrace: Boolean = false
    ): WiredException = WiredException(
            className = specimen.className,
            message = specimen.message,
            stacktrace = wiredStack(specimen.stack, specimen.stack.hash, fullStack, simpleTrace),
            cause = specimen.cause?.let { cause -> wiredException(cause, fullStack) })

    private fun wiredStack(
            stack: ThrowableStack,
            stacktraceRef: UUID,
            fullStack: Boolean = false,
            simpleTrace: Boolean = false
    ): WiredStackTrace {
        return WiredStackTrace(
                stack.className,
                if (fullStack)
                    wiredStackTrace(stack.stackTrace)
                else
                    emptyList(),
                if (simpleTrace && !fullStack)
                    simpleStackTrace(stack.stackTrace)
                else
                    emptyList(),
                stacktraceRef)
    }

    private fun wiredStackTrace(
            stackTrace: List<StackTraceElement>
    ): List<WiredStackTraceElement>? =
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

    private fun simpleStackTrace(stackTrace: List<StackTraceElement>): List<String> =
            stackTrace.map { it.toString() }
}
