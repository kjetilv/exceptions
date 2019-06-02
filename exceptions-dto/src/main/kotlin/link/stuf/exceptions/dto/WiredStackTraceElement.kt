package link.stuf.exceptions.dto

data class WiredStackTraceElement(
        val classLoaderName: String?,
        val moduleName: String?,
        val moduleVersion: String?,
        val declaringClass: String,
        val methodName: String,
        val fileName: String?,
        val lineNumber: Int?
)
