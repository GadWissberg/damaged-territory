package com.gadarts.returnfire.components.onboarding

import com.badlogic.ashley.core.Entity
import com.gadarts.returnfire.managers.GameAssetManager
import com.gadarts.returnfire.managers.SoundPlayer

interface BoardingAnimation {
    fun update(deltaTime: Float, character: Entity, soundPlayer: SoundPlayer, assetsManager: GameAssetManager): Boolean
    fun isDone(): Boolean
    fun init(stage: Entity?)

}
