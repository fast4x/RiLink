package it.fast4x.riviewer

import android.content.Context
import it.fast4x.riviewer.utils.lastVideoIdKey
import it.fast4x.riviewer.utils.lastVideoSecondsKey
import it.fast4x.riviewer.utils.logDebugEnabledKey
import it.fast4x.riviewer.utils.preferences

fun appContext(): Context = Dependencies.application.applicationContext
fun context(): Context = Dependencies.application

fun getLastYTVideoId() = appContext().preferences.getString(lastVideoIdKey, "")
fun getLastYTVideoSeconds() = appContext().preferences.getFloat(lastVideoSecondsKey, 0f)
fun isDebugModeEnabled() = appContext().preferences.getBoolean(logDebugEnabledKey, false)