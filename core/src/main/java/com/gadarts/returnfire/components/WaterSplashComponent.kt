package com.gadarts.returnfire.components

import com.badlogic.gdx.utils.TimeUtils

class WaterSplashComponent : GameComponent() {
    var creationTime: Long = 0
        private set

    override fun reset() {

    }

    fun init() {
        this.creationTime = TimeUtils.millis()
    }

}