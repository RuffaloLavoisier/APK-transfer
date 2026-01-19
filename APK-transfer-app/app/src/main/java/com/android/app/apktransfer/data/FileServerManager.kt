package com.android.app.apktransfer.data

import com.android.app.apktransfer.server.SimpleFileServer
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

class FileServerManager {
    private var server: SimpleFileServer? = null

    fun start(files: List<File>): Result<String> {
        if (files.isEmpty()) {
            return Result.failure(IllegalStateException("No files selected"))
        }

        val ip = getLocalIpAddress() ?: return Result.failure(IllegalStateException("No WiFi"))
        return runCatching {
            server = SimpleFileServer(8080, files).also { it.start() }
            "http://$ip:8080"
        }
    }

    fun stop() {
        server?.stop()
        server = null
    }

    fun isRunning(): Boolean = server != null

    private fun getLocalIpAddress(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
