package com.gadarts.returnfire.managers

import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.gadarts.returnfire.factories.Factories
import com.gadarts.returnfire.screens.ScreensManager
import com.gadarts.returnfire.systems.ai.MapPathFinder
import com.gadarts.returnfire.systems.data.StainsHandler
import com.gadarts.shared.GameAssetManager

data class GamePlayManagers(
    val soundManager: SoundManager,
    val assetsManager: GameAssetManager,
    val dispatcher: MessageDispatcher,
    val factories: Factories,
    val screensManager: ScreensManager,
    val ecs: EcsManager,
    val stainsHandler: StainsHandler,
    val pathFinder: MapPathFinder
)
