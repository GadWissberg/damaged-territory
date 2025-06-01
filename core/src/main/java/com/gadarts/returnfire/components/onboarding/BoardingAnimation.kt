package com.gadarts.returnfire.components.onboarding

import com.badlogic.ashley.core.Entity
import com.gadarts.returnfire.managers.SoundManager
import com.gadarts.shared.GameAssetManager

interface BoardingAnimation {
    fun update(
        deltaTime: Float,
        character: Entity,
        soundManager: SoundManager,
        assetsManager: GameAssetManager
    ): Boolean
    fun isDone(): Boolean
    fun init(stage: Entity?)

}
