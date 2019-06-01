package link.stuf.exceptions.server

import io.swagger.v3.oas.models.*
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import link.stuf.exceptions.server.api.Submission
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.util.*

object SwaggerJson : () -> OpenAPI {

    override fun invoke(): OpenAPI = OpenAPI().apply {
        openapi = "3.0.1"
        info = Info().apply {
            title = "Exceptions"
            description = "A server that deals with your exceptions"
            version = "0.1.0"
            license = License().apply {
                name = "GPLv3"
            }
        }
        components = Components().apply {
            addSchemas("Submission", Schema<Submission>().apply {
                description = "Identifiers for an exception species and a specimen"
                example = exampleSubmission()
                `$ref` = "Submission"
            })
            addSchemas("Exception", Schema<String>().apply {
                description = "Exception printout"
                example = exampleException()
                `$ref` = "Exception"
            })
            addSchemas("WiredException", Schema<String>().apply {
                description = "On-the-wire Exception representation"
                example = exampleException()
                `$ref` = "WiredException"
            })
        }
        paths = Paths().apply {
            put("/submit", PathItem().apply {
                summary = "Submitting data"
                description = "How to submit data for analysis"
                post = Operation().apply {
                    summary = "Submit an exception"
                    description = "Submit an exception"
                    operationId = "submit"
                    requestBody = RequestBody().apply {
                        description = "Exception printout"
                        content = Content().apply {
                            put("text/plain", MediaType().apply {
                                example = exampleException()
                                schema = Schema<String>().apply {
                                    `$ref` = "Exception"
                                }

                            })
                        }
                        required = true
                        `$ref` = "Exception"
                    }
                    responses = ApiResponses().apply {
                        put("Unique UUIDs", ApiResponse().apply {
                            description = "UUIDs species/specimen"
                            content = Content().apply {
                                put("application/json",
                                        MediaType().apply {
                                            example = exampleSubmission()
                                            schema = Schema<Submission>().apply {
                                                `$ref` = "Submission"
                                            }
                                        }
                                )
                                `$ref` = "Submission"
                            }
                        })
                    }
                }
            })
        }
    }

    private fun exampleSubmission() =
            Submission(UUID.randomUUID(), UUID.randomUUID())

    private fun exampleException(): String =
            ByteArrayOutputStream().let { baos ->
                IllegalStateException().printStackTrace(PrintWriter(baos))
                baos.close()
                baos.toByteArray()
            }.let { bytes ->
                String(bytes)
            }
}
