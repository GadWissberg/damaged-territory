package com.gadarts.returnfire.managers

import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.gadarts.returnfire.factories.Factories
import com.gadarts.returnfire.screens.ScreensManager
import com.gadarts.returnfire.systems.data.StainsHandler

class GamePlayManagers(
    val soundPlayer: SoundPlayer,
    val assetsManager: GameAssetManager,
    val dispatcher: MessageDispatcher,
    val factories: Factories,
    val screensManager: ScreensManager,
    val ecs: EcsManager,
    val stainsHandler: StainsHandler
)
