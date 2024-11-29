package com.gadarts.returnfire.components.onboarding

import com.badlogic.ashley.core.Entity
import com.gadarts.returnfire.SoundPlayer
import com.gadarts.returnfire.assets.GameAssetManager

interface BoardingAnimation {
    fun update(deltaTime: Float, character: Entity, soundPlayer: SoundPlayer, assetsManager: GameAssetManager): Boolean

}
