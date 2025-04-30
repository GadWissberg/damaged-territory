package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Component

class DrowningEffectComponent : Component {
    var lastSplashTime: Long = 0
        private set

    fun refreshLastSplashTime() {
        lastSplashTime = System.currentTimeMillis()
    }
}
