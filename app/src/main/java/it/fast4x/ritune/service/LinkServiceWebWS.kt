package it.fast4x.ritune.service

import android.app.Activity
import android.content.Context
import android.net.wifi.WifiManager
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds
import io.ktor.network.tls.certificates.buildKeyStore
import io.ktor.network.tls.certificates.saveToFile
import io.ktor.server.netty.Netty
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import it.fast4x.ritune.appContext
import it.fast4x.ritune.models.PlayerState
import it.fast4x.ritune.models.RemoteCommand
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.json.Json

class LinkServiceWebWS(
    private val activity: Activity,
    private val onCommandLoad: (mediaId: String, position: Float) -> Unit,
    private val onCommandPlay: () -> Unit,
    private val onCommandPause: () -> Unit,
    private val onCommandSeek: (Float) -> Unit
) {

    private val connections = ConcurrentHashMap<DefaultWebSocketSession, Unit>()

    private val server by lazy {

        embeddedServer(Netty, configure = {

            // Extra connectors
//            connectors.add(EngineConnectorBuilder().apply {
//                //host = "127.0.0.1"
//                port = 8080
//            })

            //envConfigWithoutSSL()
            envConfigWithSSL()

            connectionGroupSize = 2
            workerGroupSize = 5
            callGroupSize = 10
            shutdownGracePeriod = 2000
            shutdownTimeout = 3000
        }) {
            install(WebSockets) {
                pingPeriod = 15.seconds
                timeout = 15.seconds
                maxFrameSize = Long.MAX_VALUE
            }
            module()
        }
    }

    fun stop() {
        isServiceRunning = false
        server.stop(1000, 5000)
    }

    fun Application.module() {
        install(IgnoreTrailingSlash)

        routing {
            get("/") {
                call.respondText("RiTune Server running on ${ipAddress()}")
            }

            webSocket("/ws") {
                connections[this] = Unit
                Timber.d("RiTune LinkserviceWebWS: Controller connesso. Totale connessi: ${connections.size}")

                try {
                    currentState?.let {
                        val json = Json.encodeToString(it)
                        send(json)
                    }

                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val receivedText = frame.readText()
                            handleCommand(receivedText)
                        }
                    }
                } catch (e: ClosedReceiveChannelException) {
                    Timber.d("RiTune LinkserviceWebWS: Controller disconnesso")
                } catch (e: Exception) {
                    Timber.e(e, "RiTune LinkserviceWebWS: Errore WebSocket")
                } finally {
                    connections.remove(this)
                }
            }
        }
    }

    private suspend fun handleCommand(jsonString: String) {
        try {
            val cmd = Json.decodeFromString<RemoteCommand>(jsonString)
            Timber.d("RiTune LinkserviceWebWS: Comando ricevuto: $cmd")

            when (cmd.action) {
                "load" -> cmd.mediaId?.let { onCommandLoad(it, cmd.position ?: 0f) }
                "play" -> onCommandPlay()
                "pause" -> onCommandPause()
                "seek" -> cmd.position?.let { onCommandSeek(it) }
                "sync" -> currentState?.let {
                    currentState?.let { broadcastState(it) }
                }
            }
        } catch (e: Exception) {
            Timber.e("RiTune LinkserviceWebWS: Errore parsing comando: ${e.message}")
        }
    }

//    private suspend fun DefaultWebSocketSession.sendState(state: PlayerState) {
//        try {
//            val jsonString = Json.encodeToString(state)
//            send(jsonString)
//        } catch (e: Exception) {
//            Timber.e(e, "LinkserviceWebWS: Errore invio stato sessione")
//        }
//    }

    suspend fun broadcastState(state: PlayerState) {
        currentState = state
        val jsonString = Json.encodeToString(state)

        for (session in connections.keys) {
            try {
                session.send(jsonString)
            } catch (e: Exception) {
                Timber.e("RiTune LinkserviceWebWS: Errore invio stato a client: ${e.message}")
                // Opzionale: rimuovi la sessione se l'invio fallisce
                // connections.remove(session)
            }
        }
    }

    fun ApplicationEngine.Configuration.envConfigWithoutSSL() {

        connector {
            host = "0.0.0.0"
            port = 13456
        }
        connector {
            host = "0.0.0.0"
            port = 9090
        }
    }

    fun ApplicationEngine.Configuration.envConfigWithSSL() {
        // Configurazione HTTP standard
        connector { port = 18000 }

        val currentIp = ipAddress ?: "127.0.0.1"
        Timber.d("RiTune LinkserviceWebWS: Generazione certificato per IP: $currentIp") // Verifica questo nel Logcat

        // Percorso del file certificato
        val keyStoreFile = File("${appContext().externalCacheDir?.absolutePath}/build/keystore.jks")

        // Assicurati che la cartella esista
        if (keyStoreFile.parentFile?.exists() == false) {
            keyStoreFile.parentFile?.mkdirs()
        }

        val keyStore = buildKeyStore {
            certificate("riPlayAlias") {
                password = "biPwd1@"
                domains = listOf(
                    "127.0.0.1",
                    "0.0.0.0",
                    "localhost",
                    // AGGIUNGI QUESTO RIGA:
                    ipAddress ?: "0.0.0.0"
                )
            }
        }
        keyStore.saveToFile(keyStoreFile, "123456")

        sslConnector(
            keyStore = keyStore,
            keyAlias = "riPlayAlias",
            keyStorePassword = { "wo0567#".toCharArray() },
            privateKeyPassword = { "biPwd1@".toCharArray() }
        ) {
            port = 18443
            keyStorePath = keyStoreFile
        }
    }

    fun start() {
        ipAddress = findIPAddress(activity)
        isServiceRunning = true
        CoroutineScope(Dispatchers.IO).launch {
            server.start(wait = true)
        }
    }

    private fun findIPAddress(context: Context): String? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return try {
            if (wifiManager.connectionInfo != null) {
                val wifiInfo = wifiManager.connectionInfo
                InetAddress.getByAddress(
                    ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                        .putInt(wifiInfo.ipAddress).array()
                ).hostAddress
            } else null
        } catch (e: Exception) {
            Timber.e("RiTune LinkserviceWebWS:  Error finding IpAddress: ${e.message}")
            null
        }
    }

    @Synchronized fun isServiceRunning() = isServiceRunning
    @Synchronized fun ipAddress() = ipAddress

    companion object {
        private var isServiceRunning = false
        private var ipAddress: String? = null
        private var currentState: PlayerState? = null
    }
}