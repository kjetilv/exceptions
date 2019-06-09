package link.stuf.exceptions.server

data class ServerConfiguration(
        val prefix: String = "/api/v1",
        val host: String = "0.0.0.0",
        val port: Int = 8080,
        val selfDiagnose: Boolean = true
)
