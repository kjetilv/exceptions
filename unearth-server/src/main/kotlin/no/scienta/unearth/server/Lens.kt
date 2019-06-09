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
 *     along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package no.scienta.unearth.server

import io.swagger.v3.oas.models.OpenAPI
import no.scienta.unearth.core.parser.ThrowableParser
import no.scienta.unearth.dto.FaultTypeDto
import no.scienta.unearth.dto.FaultEventDto
import no.scienta.unearth.dto.Submission
import no.scienta.unearth.dto.CauseDto
import no.scienta.unearth.munch.util.Throwables
import no.scienta.unearth.server.JSON.auto
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.lens.BiDiBodyLens
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.string

object Lens {

    val submission =
            Body.auto<Submission>(contentNegotiation = ContentNegotiation.None).toLens()

    val faultEvent =
            Body.auto<FaultEventDto>(contentNegotiation = ContentNegotiation.None).toLens()

    val exception: BiDiBodyLens<Throwable> = BiDiBodyLens(
            metas = emptyList(),
            contentType = TEXT_PLAIN,
            get = { msg ->
                msg.let { ThrowableParser.parse(it.body.payload) }
            },
            setLens = { thr, msg ->
                msg.body(Throwables.string(thr))
            })

    val string = Body.string(TEXT_PLAIN).toLens()

    val faultType =
            Body.auto<FaultTypeDto>(contentNegotiation = ContentNegotiation.None).toLens()

    val cause =
            Body.auto<CauseDto>(contentNegotiation = ContentNegotiation.None).toLens()

    val swagger =
            Body.auto<OpenAPI>(contentNegotiation = ContentNegotiation.None).toLens()

}
