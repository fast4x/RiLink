package it.fast4x.ritune

import android.content.Context
import it.fast4x.ritune.utils.lastVideoIdKey
import it.fast4x.ritune.utils.lastVideoSecondsKey
import it.fast4x.ritune.utils.logDebugEnabledKey
import it.fast4x.ritune.utils.preferences

fun appContext(): Context = Dependencies.application.applicationContext
fun context(): Context = Dependencies.application

fun getLastYTVideoId() = appContext().preferences.getString(lastVideoIdKey, "")
fun getLastYTVideoSeconds() = appContext().preferences.getFloat(lastVideoSecondsKey, 0f)
fun isDebugModeEnabled() = appContext().preferences.getBoolean(logDebugEnabledKey, false)