@file:JvmName("Lwjgl3Launcher")

package com.gadarts.returnfire.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.gadarts.returnfire.OpenFire

fun main() {
    if (StartupHelper.startNewJvmIfRequired())
        return
    Lwjgl3Application(OpenFire(false), Lwjgl3ApplicationConfiguration().apply {
        setTitle("open-fire")
        setWindowedMode(640, 480)
        setWindowIcon(*(arrayOf(128, 64, 32, 16).map { "libgdx$it.png" }.toTypedArray()))
    })
}
