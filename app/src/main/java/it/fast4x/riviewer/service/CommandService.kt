package it.fast4x.riviewer.service

import android.app.Activity
import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

class CommandService(
    private val activity: Activity,
    onCommandLoad: (mediaId: String, position: Float) -> Unit,
    onCommandPlay: () -> Unit,
    onCommandPause: () -> Unit
) {

    val onCommandLoad: (mediaId: String, position: Float) -> Unit = { id, position ->
        onCommandLoad(id, position)
    }
    val onCommandPlay: () -> Unit = {
        onCommandPlay()
    }
    val onCommandPause: () -> Unit = {
        onCommandPause()
    }

    fun String.cleanParam(): String? = this.replace("HTTP/1.1", "").replace(" ", "")

    fun stop() {
        isServiceSunning = false
        try {
            if (serviceSocket != null) {
                serviceSocket!!.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    suspend fun start() {
        isServiceSunning = true
        ipAddress = findIPAddress(activity)
        val selectorManager = SelectorManager(Dispatchers.IO)
        serviceSocket = aSocket(selectorManager).tcp().bind(ipAddress ?: "127.0.0.1", 8000)
        println("CommandService is listening at ${serviceSocket?.localAddress}")
        while (isServiceSunning) {
            val socket = serviceSocket?.accept()
            println("CommandService Accepted $socket")
            CoroutineScope(Dispatchers.IO).launch {
                val receiveChannel = socket?.openReadChannel()
                val sendChannel = socket?.openWriteChannel(autoFlush = true)
                sendChannel?.writeStringUtf8("CommandService Hello Please enter your name\n")
                try {
                    while (isServiceSunning) {
                        val receivedInput = receiveChannel?.readUTF8Line()
                        println("CommandService received input $receivedInput")
                        val parameters = receivedInput?.split("|")
                        println("CommandService received parameters $parameters")
                        val command = parameters?.getOrNull(0)
                        val requestMediaId = parameters?.getOrNull(1)
                        val position = parameters?.getOrNull(2)
                        if (command != null) {
                            if (command == commandLoad) {
                                if (requestMediaId?.isNotEmpty() == true) {
                                    onCommandLoad(requestMediaId.toString(), position?.toFloat() ?: 0f)
                                    println("CommandService received play mediaId $requestMediaId position")
                                }
                            }
                            if (command == commandPlay) {
                                onCommandPlay()
                                println("CommandService received play")
                            }
                            if (command == commandPause) {
                                onCommandPause()
                                println("CommandService received pause")
                            }

                        }

                        //sendChannel?.writeStringUtf8("Hello, $name!\n")
                    }
                } catch (e: Throwable) {
                    println("CommandService Socket closed ${e.message}")
                    isServiceSunning = false
                    socket?.close()
                }
            }
        }
    }

    @Synchronized
    fun ipAddress(): String? = ipAddress
    @Synchronized
    fun isServiceSunning(): Boolean = isServiceSunning
    @Synchronized
    fun mediaId(): String = mediaId

    private fun findIPAddress(context: Context): String? {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        try {
            return if (wifiManager.connectionInfo != null) {
                val wifiInfo = wifiManager.connectionInfo
                InetAddress.getByAddress(
                    ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                        .putInt(wifiInfo.ipAddress)
                        .array()
                ).hostAddress
            } else
                null
        } catch (e: Exception) {
            Log.e(CommandService::class.java.simpleName, "Error finding IpAddress: ${e.message}", e)
        }
        return null
    }

    companion object {
        private var serviceSocket: io.ktor.network.sockets.ServerSocket? = null
        private var isServiceSunning = false
        private var ipAddress: String? = null
        private var port: String? = null
        var mediaId: String = ""
        const val commandLoad: String = "load"
        const val commandPlay: String = "play"
        const val commandPause: String = "pause"
    }
}