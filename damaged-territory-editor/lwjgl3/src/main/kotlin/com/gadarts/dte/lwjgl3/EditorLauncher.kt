@file:JvmName("Lwjgl3Launcher")

package com.gadarts.dte.lwjgl3

import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.gadarts.dte.DamagedTerritoryEditor
import com.gadarts.dte.EditorEvents

/** Launches the desktop (LWJGL3) application. */
fun main() {
    if (StartupHelper.startNewJvmIfRequired())
      return
    val dispatcher = MessageDispatcher()
    dispatcher.addListener(DesktopListener(), EditorEvents.SAVE_MAP.ordinal)
    Lwjgl3Application(DamagedTerritoryEditor(dispatcher), Lwjgl3ApplicationConfiguration().apply {
        setTitle("damaged-territory-editor")
        useVsync(true)
        setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1)
        setWindowedMode(1280, 960)
    })
}
