package com.gadarts.returnfire.components.cd

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureRegion

class DecalAnimation(frameDuration: Float, frames: com.badlogic.gdx.utils.Array<TextureRegion>) {
    private var stateTime: Float = 0.0f

    private val animation = Animation(frameDuration, frames, Animation.PlayMode.LOOP)

    fun calculateNextFrame(deltaTime: Float): TextureRegion {
        stateTime += deltaTime
        return animation.getKeyFrame(stateTime)
    }
}
