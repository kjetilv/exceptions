package link.stuf.exceptions.server

import io.swagger.v3.oas.models.*
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import link.stuf.exceptions.dto.Submission
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.util.*

object SwaggerJson : () -> OpenAPI {

    override fun invoke(): OpenAPI = OpenAPI().apply {
        openapi = "3.0.1"
        info = Info().apply {
            version = "0.1.0"
            title = "Exceptions"
            description = "A server that deals with your exceptions"
            license = License().apply {
                name = "GPL v3"
                url = "https://www.gnu.org/licenses/gpl-3.0.txt"
            }
        }
        components = Components().apply {
            schemas = mapOf(
                    "UUID" to Schema<String>().apply {
                        type = "string"
                        description = "UUID"
                        example = exampleException()
                    },
                    "Submission" to Schema<Submission>().apply {
                        required = listOf("speciesId", "specimenId")
                        properties = mapOf(
                                "speciesId" to Schema<String>().apply {
                                    `$ref` = "UUID"
                                },
                                "specimenId" to Schema<String>().apply {
                                    `$ref` = "UUID"
                                })
                        description = "Identifiers for an exception species and a specimen"
                        example = exampleSubmission()
                    },
                    "Exception" to Schema<String>().apply {
                        type = "String"
                        description = "Exception printout"
                        example = exampleException()
                    },
                    "WiredException" to Schema<String>().apply {
                        type = "Object"
                        description = "On-the-wire Exception representation"
                        example = exampleException()
                    })
            parameters = mapOf(
                    "UUID" to Parameter().apply {
                        name = "uuid"
                        description = "UUID for an exception"
                        schema = Schema<String>().apply {
                            type = "String"
                            example = exampleUuid()
                        }
                    }
            )
            requestBodies = mapOf(
                    "Exception" to RequestBody().apply {
                        required = true
                        description = "An exception printout"
                        content = Content().apply {
                            put("text/plain", MediaType().apply {
                                example = exampleException()
                                schema = Schema<String>().apply {
                                    `$ref` = "Exception"
                                }
                            })
                        }
                    }
            )
        }
        paths = Paths().apply {
            addPathItem("/exceptions", PathItem().apply {
                summary = "Storing and retrieving exceptions"
                get = Operation().apply {
                    summary = "Retrieve an exception"
                    operationId = "exceptionGet"
                    parameters = listOf(Parameter().apply {
                        `in` = "path"
                        `$ref` = "UUID"
                        required = true
                    })
                }
                post = Operation().apply {
                    summary = "Submit an exception"
                    operationId = "exceptionSubmit"
                    requestBody = RequestBody().apply {
                        description = "Exception printout"
                        required = true
                        `$ref` = "Exception"
                    }
                    responses = ApiResponses().apply {
                        addApiResponse("Unique UUIDs", ApiResponse().apply {
                            description = "UUIDs species/specimen"
                            content = Content().apply {
                                `$ref` = "Submission"
                                put("application/json", MediaType().apply {
                                    example = exampleSubmission()
                                    schema = Schema<Submission>().apply {
                                        `$ref` = "Submission"
                                    }
                                })
                            }
                        })
                    }
                }
            })
        }
    }

    private fun exampleUuid() = UUID.randomUUID().toString()

    private fun exampleSubmission() = Submission(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())

    private fun exampleException(): String =
            ByteArrayOutputStream().let { baos ->
                IllegalStateException().printStackTrace(PrintWriter(baos))
                baos.close()
                baos.toByteArray()
            }.let { bytes ->
                String(bytes)
            }
}
