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

package no.scienta.unearth.server

import no.scienta.unearth.core.HandlingPolicy
import no.scienta.unearth.munch.model.FrameFun
import no.scienta.unearth.munch.print.*
import java.util.stream.Stream

class UnearthlyRenderer {

    private val renderers: Map<HandlingPolicy.Action, CausesRenderer> = hashMapOf(
            HandlingPolicy.Action.LOG to SimpleCausesRenderer(ConfigurableStackRenderer()),
            HandlingPolicy.Action.LOG_SHORT to SimpleCausesRenderer(rendererFor("org.http4k", "io.netty")),
            HandlingPolicy.Action.LOG_MESSAGES to SimpleCausesRenderer(ConfigurableStackRenderer().noStack()))

    fun rendering(policy: HandlingPolicy): CausesRendering? = policy.action
            ?.let { renderers[it] }
            ?.let { it.render(policy.fault) }

    private fun rendererFor(vararg groups: String) =
            rendererFor(groups.toList())

    private fun rendererFor(groups: List<String>): StackRenderer =
            ConfigurableStackRenderer()
                    .group(SimplePackageGrouper(groups))
                    .squash { _, causeFrames ->
                        Stream.of(" * [${causeFrames.size} hidden]")
                    }
                    .reshape(FrameFun.LIKE_JAVA_8)
                    .reshape(FrameFun.SHORTEN_CLASSNAMES)
}
