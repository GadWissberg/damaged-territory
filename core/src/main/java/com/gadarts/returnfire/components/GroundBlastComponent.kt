package com.gadarts.returnfire.components

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.TimeUtils

class GroundBlastComponent : GameComponent() {
    var fadeOutPace: Float = 0F
        private set
    var duration: Int = 0
        private set
    var scalePace: Float = 0.0f
        private set
    var creationTime: Long = 0
        private set

    override fun reset() {

    }

    fun init(scalePace: Float, duration: Int, fadeOutPace: Float) {
        this.creationTime = TimeUtils.millis()
        this.scalePace = MathUtils.random(scalePace, scalePace + 0.01F)
        this.duration = duration
        this.fadeOutPace = fadeOutPace
    }

}
