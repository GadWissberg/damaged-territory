package com.gadarts.returnfire

import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.screens.ScreensManager
import com.gadarts.returnfire.systems.SpecialEffectsGenerator
import com.gadarts.returnfire.systems.data.pools.RigidBodyFactory

class Managers(
    val engine: PooledEngine,
    val soundPlayer: SoundPlayer,
    val assetsManager: GameAssetManager,
    val dispatcher: MessageDispatcher,
    val rigidBodyFactory: RigidBodyFactory,
    val specialEffectsGenerator: SpecialEffectsGenerator,
    val screensManager: ScreensManager
)
