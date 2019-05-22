package link.stuf.exceptions.server.api

data class WiredStackTraceElement(

        val classLoaderName: String?,

        val moduleName: String?,

        val moduleVersion: String?,

        val declaringClass: String,

        val methodName: String,

        val fileName: String?,

        val lineNumber: Int?
)
