package com.android.app.apktransfer.server

import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream

class SimpleFileServer(port: Int, private val files: List<File>) : NanoHTTPD(port) {
    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        return when {
            uri == "/" -> {
                val html = buildString {
                    append("<!DOCTYPE html><html><head>")
                    append("<meta charset='UTF-8'>")
                    append("<meta name='viewport' content='width=device-width, initial-scale=1'>")
                    append("<title>APK File Transfer</title>")
                    append("<style>")
                    append("body{font-family:Arial;margin:20px;background:#0F0F1E;color:#fff}")
                    append("h1{color:#4FACFE}")
                    append(".file-item{background:#1A1A2E;padding:15px;margin:10px 0;border-radius:8px}")
                    append("a{color:#4FACFE;text-decoration:none;font-size:16px}")
                    append("</style></head><body>")
                    append("<h1>📱 Available Files (${files.size})</h1>")
                    files.forEachIndexed { index, file ->
                        val sizeMB = file.length() / 1024.0 / 1024.0
                        append("<div class='file-item'>")
                        append("<a href='/download/$index'>${file.name}</a>")
                        append("<div>📦 ${"%.2f".format(sizeMB)} MB</div>")
                        append("</div>")
                    }
                    append("</body></html>")
                }
                newFixedLengthResponse(Response.Status.OK, "text/html", html)
            }
            uri.startsWith("/download/") -> {
                val index = uri.substringAfter("/download/").toIntOrNull()
                if (index != null && index in files.indices) {
                    val file = files[index]
                    if (file.exists()) {
                        val fis = FileInputStream(file)
                        val response = newFixedLengthResponse(
                            Response.Status.OK,
                            "application/octet-stream",
                            fis,
                            file.length()
                        )
                        response.addHeader("Content-Disposition", "attachment; filename=\"${file.name}\"")
                        response
                    } else {
                        newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found")
                    }
                } else {
                    newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Invalid file index")
                }
            }
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found")
        }
    }
}
