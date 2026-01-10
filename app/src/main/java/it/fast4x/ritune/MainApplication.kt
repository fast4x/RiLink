package it.fast4x.ritune

import android.app.Application
import it.fast4x.ritune.utils.CaptureCrash
import it.fast4x.ritune.utils.FileLoggingTree
import it.fast4x.ritune.utils.logDebugEnabledKey
import it.fast4x.ritune.utils.preferences
import timber.log.Timber
import java.io.File

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Dependencies.init(this)

        /***** CRASH LOG ALWAYS ENABLED *****/
        val dir = filesDir.resolve("logs").also {
            if (it.exists()) return@also
            it.mkdir()
        }
        Thread.setDefaultUncaughtExceptionHandler(CaptureCrash(dir.absolutePath))
        /***** CRASH LOG ALWAYS ENABLED *****/

        /**** LOG *********/
        val logEnabled = preferences.getBoolean(logDebugEnabledKey, false)
        if (logEnabled) {
            Timber.plant(FileLoggingTree(File(dir, "RiPlayLink_log.txt")))
            Timber.d("Log enabled at ${dir.absolutePath}")
        } else {
            Timber.uprootAll()
            Timber.plant(Timber.DebugTree())
        }
        /**** LOG *********/
    }

}

object Dependencies {
    lateinit var application: MainApplication
        private set

    internal fun init(application: MainApplication) {
        this.application = application
    }
}