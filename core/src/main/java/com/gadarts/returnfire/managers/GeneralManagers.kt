package com.gadarts.returnfire.managers

import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.gadarts.returnfire.screens.ScreensManager

class GeneralManagers(
    val assetsManager: GameAssetManager,
    val soundManager: SoundManager,
    val dispatcher: MessageDispatcher,
    val screensManagers: ScreensManager
)
