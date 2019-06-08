package link.stuf.exceptions.server

import io.swagger.v3.oas.models.OpenAPI
import link.stuf.exceptions.core.parser.ThrowableParser
import link.stuf.exceptions.dto.Species
import link.stuf.exceptions.dto.Specimen
import link.stuf.exceptions.dto.Submission
import link.stuf.exceptions.dto.WiredStackTrace
import link.stuf.exceptions.munch.Throwables
import link.stuf.exceptions.server.JSON.auto
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.lens.BiDiBodyLens
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.string

object Lens {

    val submission =
            Body.auto<Submission>(contentNegotiation = ContentNegotiation.None).toLens()

    val specimen =
            Body.auto<Specimen>(contentNegotiation = ContentNegotiation.None).toLens()

    val exception: BiDiBodyLens<Throwable?> = BiDiBodyLens(
            metas = emptyList(),
            contentType = TEXT_PLAIN,
            get = { msg ->
                msg.let { ThrowableParser.parse(it.body.payload) }
            },
            setLens = { thr, msg ->
                msg.body(Throwables.string(thr))
            })

    val string = Body.string(TEXT_PLAIN).toLens()

    val species =
            Body.auto<Species>(contentNegotiation = ContentNegotiation.None).toLens()

    val stack =
            Body.auto<WiredStackTrace>(contentNegotiation = ContentNegotiation.None).toLens()

    val swagger =
            Body.auto<OpenAPI>(contentNegotiation = ContentNegotiation.None).toLens()

}
