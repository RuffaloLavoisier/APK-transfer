// 데이터 계층 패키지 선언.
package com.android.app.apktransfer.data

// 내장 HTTP 파일 서버 구현.
import com.android.app.apktransfer.server.SimpleFileServer
// 로컬 파일 참조용.
import java.io.File
// IPv4 주소 필터링용.
import java.net.Inet4Address
// 네트워크 인터페이스 열거용.
import java.net.NetworkInterface

// 단말에서 간단한 파일 서버를 켜고 끄는 매니저.
class FileServerManager {
    // 서버 인스턴스가 없으면 비가동 상태.
    private var server: SimpleFileServer? = null

    // 선택된 파일을 서빙하는 HTTP 서버를 시작한다.
    fun start(files: List<File>): Result<String> {
        // 공유할 파일이 없으면 시작하지 않는다.
        if (files.isEmpty()) {
            return Result.failure(IllegalStateException("No files selected"))
        }

        // Wi-Fi IP를 찾지 못하면 접속 URL을 만들 수 없다.
        val ip = getLocalIpAddress() ?: return Result.failure(IllegalStateException("No WiFi"))
        return runCatching {
            // 8080 포트로 간단 서버를 띄운다.
            server = SimpleFileServer(8080, files).also { it.start() }
            // 접근 가능한 URL을 반환한다.
            "http://$ip:8080"
        }
    }

    // 서버를 중지하고 참조를 해제한다.
    fun stop() {
        server?.stop()
        server = null
    }

    // 서버 가동 여부를 간단히 확인한다.
    fun isRunning(): Boolean = server != null

    // 루프백이 아닌 IPv4 주소를 찾아 반환한다.
    private fun getLocalIpAddress(): String? {
        return try {
            // 모든 네트워크 인터페이스를 순회한다.
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                // 각 인터페이스의 IP 목록을 확인한다.
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    // 127.x.x.x가 아닌 IPv4 주소만 사용한다.
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
            // 유효한 주소가 없으면 null 반환.
            null
        } catch (e: Exception) {
            // 예외 시 로그를 남기고 null 처리.
            e.printStackTrace()
            null
        }
    }
}
