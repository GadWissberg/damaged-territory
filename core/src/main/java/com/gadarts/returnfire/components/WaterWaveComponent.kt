package com.gadarts.returnfire.components

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.TimeUtils

class WaterWaveComponent : GameComponent() {
    var scalePace: Float = 0.0f
    var creationTime: Long = 0
        private set

    override fun reset() {

    }

    fun init() {
        this.creationTime = TimeUtils.millis()
        this.scalePace = MathUtils.random(1.01F, 1.02F)
    }

}
