package com.gadarts.returnfire.components.effects

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.audio.Sound

class CrashingAircraftEmitter(val soundToStop: Sound, val soundToStopId: Long) : Component {
    fun crash() {
        crashed = true
    }

    var crashed: Boolean = false
        private set
}
