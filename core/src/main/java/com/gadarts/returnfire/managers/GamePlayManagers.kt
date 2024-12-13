package com.gadarts.returnfire.managers

import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.gadarts.returnfire.factories.Factories
import com.gadarts.returnfire.screens.ScreensManager
import com.gadarts.returnfire.systems.EntityBuilder

class GamePlayManagers(
    val engine: PooledEngine,
    val soundPlayer: SoundPlayer,
    val assetsManager: GameAssetManager,
    val dispatcher: MessageDispatcher,
    val factories: Factories,
    val screensManager: ScreensManager,
    val entityBuilder: EntityBuilder
)
