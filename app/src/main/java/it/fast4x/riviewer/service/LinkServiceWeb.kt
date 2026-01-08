package it.fast4x.riviewer.service

import android.app.Activity
import android.content.Context
import android.net.wifi.WifiManager
import io.ktor.network.tls.certificates.buildKeyStore
import io.ktor.network.tls.certificates.saveToFile
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.EngineConnectorConfig
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.routing.IgnoreTrailingSlash
import it.fast4x.riviewer.appContext
import kotlinx.coroutines.runBlocking
import timber.log.Timber

//fun Application.module() {
//    install(CallLogging)
//
//    monitor.subscribe(ApplicationStarted) {
//        println("LinkServiceWeb ApplicationStarted")
//    }
//
//    monitor.subscribe(ApplicationStopped) {
//        println("LinkServiceWeb ApplicationStopped")
//    }
//
//    routing {
//        println("LinkServiceWeb routing /")
//        get("/") {
//            println("LinkServiceWeb get /")
//            call.respondText("LinkServiceWeb Hello, world from Module in ${Build.MODEL}!")
//        }
//    }
//}

//fun ApplicationEngine.Configuration.envConfig() {
//
//    connector {
//        host = "0.0.0.0"
//        port = 13456
//    }
//    connector {
//        host = "0.0.0.0"
//        port = 9090
//    }
//}
//
//fun ApplicationEngine.Configuration.envConfigWithSSL() {
//
//    println("LinkServiceWeb ApplicationEngine.Configuration envConfig")
//
//    connector {
//        port = 8000
//    }
//
//        val keyStoreFile = File("${appContext().externalCacheDir?.absolutePath}/build/keystore.jks")
//        val keyStore = buildKeyStore {
//            certificate("riPlayAlias") {
//                password = "biPwd1@"
//                domains = listOf("127.0.0.1", "0.0.0.0", "localhost")
//            }
//        }
//        keyStore.saveToFile(keyStoreFile, "123456")
//
//        sslConnector(
//            keyStore = keyStore,
//            keyAlias = "riPlayAlias",
//            keyStorePassword = { "wo0567#".toCharArray() },
//            privateKeyPassword = { "biPwd1@".toCharArray() }
//        ) {
//            port = 8443
//            keyStorePath = keyStoreFile
//        }
//}

class LinkServiceWeb(
    private val activity: Activity,
    onCommandLoad: (mediaId: String, position: Float) -> Unit,
    onCommandPlay: () -> Unit,
    onCommandPause: () -> Unit
) {

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
//            intercept(ApplicationCallPipeline.Call) {
//                println(call.request.queryParameters.entries())
//                call.respondText("Hello from Ktor, no routing! ${call.request.queryParameters.entries()}", ContentType.Text.Plain)
//            }
            module()
//            routing {
//                get("/") {
//                    call.respondText("Hello, world!")
//                }
//            }
        }
    }


//Work
//    private val server by lazy {
//        embeddedServer(Netty, port = 8000, watchPaths = emptyList()) {
//
//            install(CallLogging)
//
//            routing {
//                get("/") {
//                    call.respondText("LinkServiceWeb All good here in ${Build.MODEL}", ContentType.Text.Plain)
//                }
//            }
//        }
//    }

    val onCommandLoad: (mediaId: String, position: Float) -> Unit = { id, position ->
        onCommandLoad(id, position)
    }
    val onCommandPlay: () -> Unit = {
        onCommandPlay()
    }
    val onCommandPause: () -> Unit = {
        onCommandPause()
    }

    fun stop() {
        isServiceSunning = false
    }

    fun Application.module() {
        install(CallLogging)
        install(IgnoreTrailingSlash)
//        monitor.subscribe(ApplicationStarted) {
//            println("LinkServiceWeb ApplicationStarted")
//        }
//
//        monitor.subscribe(ApplicationStopped) {
//            println("LinkServiceWeb ApplicationStopped")
//        }

        routing {
            get("/") {
                println("LinkServiceWeb get /")
                call.respondText("LinkServiceWeb / ${call.parameters.getAll("param")}")
            }
            get("/load") {
                println("LinkServiceWeb get /load")
                val mediaId = call.request.queryParameters["mediaId"]
                val position = call.request.queryParameters["position"]
                //handle {
                onCommandLoad(mediaId ?: "", position?.toFloat() ?: 0f)
                //}
                call.respondText("LinkServiceWeb route get /load/  $mediaId $position ")
            }
            get("/playAt") {
                println("LinkServiceWeb get /playAt")
                val mediaId = call.request.queryParameters["mediaId"]
                val position = call.request.queryParameters["position"]
                //handle {
                    onCommandLoad(mediaId ?: "", position?.toFloat() ?: 0f)
                //}
                call.respondText("LinkServiceWeb route get /playAt/{mediaId} $mediaId $position")
            }
            get("/play") {
                println("LinkServiceWeb get /play")
                //handle {
                    onCommandPlay()
                //}
                call.respondText("LinkServiceWeb route get /play")
            }
            get("/pause") {
                println("LinkServiceWeb get /pause")
                //handle {
                    onCommandPause()
                //}
                call.respondText("LinkServiceWeb route get /pause")
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

        println("LinkServiceWeb ApplicationEngine.Configuration envConfig")

        connector {
            port = 18000
        }

        val keyStoreFile = File("${appContext().externalCacheDir?.absolutePath}/build/keystore.jks")
        val keyStore = buildKeyStore {
            certificate("riPlayAlias") {
                password = "biPwd1@"
                domains = listOf("127.0.0.1", "0.0.0.0", "localhost")
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
        isServiceSunning = true

        CoroutineScope(Dispatchers.IO).launch {
            server.start(wait = true)
        }

        resolvedConnectors = runBlocking { server.engine.resolvedConnectors() }

//        embeddedServer(Netty, applicationEnvironment { log = LoggerFactory.getLogger("ktor.application") }, {
//            ApplicationEngine.Configuration().envConfig()
//        }, module = Application::module).start(wait = true)

       // println("LinkServiceWeb is listening at ${ipAddress} : ${realPort}")
        println("LinkServiceWeb resolvedConnectors ${resolvedConnectors}")

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
            Timber.e("LinkServiceWeb Error finding IpAddress: ${e.message}")
        }
        return null
    }

    companion object {
        private var isServiceSunning = false
        private var ipAddress: String? = null
        private var resolvedConnectors: List<EngineConnectorConfig?> = emptyList()
        var mediaId: String = ""
    }
}