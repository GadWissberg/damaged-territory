package com.gadarts.returnfire

import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.gadarts.returnfire.assets.GameAssetManager

class Services(
    val engine: PooledEngine,
    val soundPlayer: SoundPlayer,
    val assetsManager: GameAssetManager,
    val dispatcher: MessageDispatcher
)
