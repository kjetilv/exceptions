/*
 *     This file is part of Unearth.
 *
 *     Unearth is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Unearth is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Unearth.  If not, see <https://www.gnu.org/licenses/>.
 */

package unearth.server.turbo

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.turbo.TurboFilter
import ch.qos.logback.core.spi.FilterReply
import org.slf4j.Marker
import org.slf4j.spi.LocationAwareLogger
import unearth.core.HandlingPolicy
import unearth.munch.print.CausesRenderer
import unearth.munch.print.CausesRendering
import unearth.server.UnearthlyResources

class UnearthlyTurboFilter(
    private val resources: UnearthlyResources,
    private val renderer: CausesRenderer,
    private val renderers: Map<HandlingPolicy.Action, CausesRenderer> = emptyMap()
) : TurboFilter() {

    override fun decide(
        marker: Marker?,
        logger: Logger,
        level: Level,
        format: String?,
        params: Array<Any>?,
        t: Throwable?
    ): FilterReply {
        if (t == null) {
            return FilterReply.NEUTRAL
        }
        val policy = resources.submitRaw(t, format, params)
        (logger as LocationAwareLogger).log(
            marker,
            (logger as LocationAwareLogger).name,
            locationAwareLevel(level),
            message(format, policy),
            allParameters(params, policy, rendering(policy)).toTypedArray(),
            null
        )
        return FilterReply.DENY
    }

    private fun rendering(policy: HandlingPolicy): CausesRendering? =
        policy.action.let { action ->
            policy.fault.let { fault ->
                renderer(action).render(fault)
            }
        }

    private fun renderer(action: HandlingPolicy.Action): CausesRenderer {
        return renderers[action] ?: renderer
    }

    fun withRendererFor(action: HandlingPolicy.Action, renderer: CausesRenderer): UnearthlyTurboFilter =
        UnearthlyTurboFilter(resources, renderer, renderers.plus(Pair(action, renderer)))

    companion object {

        private fun allParameters(params: Array<Any>?, policy: HandlingPolicy, rendering: CausesRendering?) = listOfNotNull(
            params,
            policy.faultId,
            policy.feedEntryId,
            rendering?.getStrings("  ")?.joinToString("\n")
        )

        private fun message(format: String?, policy: HandlingPolicy): String =
            "${format?.let { "$format " } ?: ""}{} {}${if (policy.`is`(HandlingPolicy.Action.LOG_ID)) "" else "\n{}"}"

        private fun locationAwareLevel(level: Level): Int = when (level) {
            Level.INFO -> LocationAwareLogger.INFO_INT
            Level.WARN -> LocationAwareLogger.WARN_INT
            Level.DEBUG -> LocationAwareLogger.DEBUG_INT
            Level.ERROR -> LocationAwareLogger.ERROR_INT
            else -> LocationAwareLogger.TRACE_INT
        }
    }

}
