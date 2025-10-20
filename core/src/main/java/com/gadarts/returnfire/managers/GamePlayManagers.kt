package com.gadarts.returnfire.managers

import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.gadarts.returnfire.ecs.systems.ai.logic.path.MapPathFinder
import com.gadarts.returnfire.ecs.systems.data.StainsManager
import com.gadarts.returnfire.factories.Factories
import com.gadarts.returnfire.screens.ScreensManager
import com.gadarts.shared.GameAssetManager

data class GamePlayManagers(
    val soundManager: SoundManager,
    val assetsManager: GameAssetManager,
    val dispatcher: MessageDispatcher,
    val factories: Factories,
    val screensManager: ScreensManager,
    val ecs: EcsManager,
    val stainsManager: StainsManager,
    val pathFinder: MapPathFinder,
)
