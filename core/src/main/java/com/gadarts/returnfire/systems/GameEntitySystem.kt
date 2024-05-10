package com.gadarts.returnfire.systems

import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.SoundPlayer
import com.gadarts.returnfire.assets.GameAssetManager

abstract class GameEntitySystem : Disposable, EntitySystem() {
    lateinit var assetsManager: GameAssetManager
    lateinit var soundPlayer: SoundPlayer
    lateinit var commonData: GameSessionData

    abstract fun initialize(am: GameAssetManager)
    abstract fun resume(delta: Long)
}
