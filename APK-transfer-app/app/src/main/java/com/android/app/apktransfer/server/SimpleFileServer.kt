// 서버 계층 패키지 선언.
package com.android.app.apktransfer.server

// 경량 HTTP 서버 라이브러리.
import fi.iki.elonen.NanoHTTPD
// 서빙 대상 파일 객체.
import java.io.File
// 파일 스트림으로 다운로드 응답을 생성.
import java.io.FileInputStream

// 간단한 파일 목록/다운로드를 제공하는 HTTP 서버.
class SimpleFileServer(port: Int, private val files: List<File>) : NanoHTTPD(port) {
    // 요청 URI에 따라 목록 페이지 또는 파일 다운로드를 처리한다.
    override fun serve(session: IHTTPSession): Response {
        // 요청 경로를 추출한다.
        val uri = session.uri
        return when {
            // 루트는 파일 목록 HTML을 반환한다.
            uri == "/" -> {
                // 간단한 인라인 HTML 페이지를 생성한다.
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
                    // 파일 리스트를 링크로 렌더링한다.
                    files.forEachIndexed { index, file ->
                        val sizeMB = file.length() / 1024.0 / 1024.0
                        append("<div class='file-item'>")
                        append("<a href='/download/$index'>${file.name}</a>")
                        append("<div>📦 ${"%.2f".format(sizeMB)} MB</div>")
                        append("</div>")
                    }
                    append("</body></html>")
                }
                // HTML 응답 반환.
                newFixedLengthResponse(Response.Status.OK, "text/html", html)
            }
            // 다운로드 경로는 /download/{index} 형태다.
            uri.startsWith("/download/") -> {
                // 인덱스를 숫자로 파싱한다.
                val index = uri.substringAfter("/download/").toIntOrNull()
                if (index != null && index in files.indices) {
                    val file = files[index]
                    if (file.exists()) {
                        // 파일을 스트림으로 응답한다.
                        val fis = FileInputStream(file)
                        val response = newFixedLengthResponse(
                            Response.Status.OK,
                            "application/octet-stream",
                            fis,
                            file.length()
                        )
                        // 다운로드 파일명 헤더 지정.
                        response.addHeader("Content-Disposition", "attachment; filename=\"${file.name}\"")
                        response
                    } else {
                        // 파일이 없으면 404 응답.
                        newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found")
                    }
                } else {
                    // 잘못된 인덱스는 400 처리.
                    newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Invalid file index")
                }
            }
            // 그 외 경로는 404 처리.
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found")
        }
    }
}
