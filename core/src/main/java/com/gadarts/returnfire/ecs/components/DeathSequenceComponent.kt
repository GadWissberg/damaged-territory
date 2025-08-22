package com.gadarts.returnfire.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.TimeUtils

class DeathSequenceComponent(val createExplosionsAround: Boolean, minimumExplosions: Int, maximumExplosions: Int) :
    Component {
    var deathSequenceNextExplosion: Long = TimeUtils.millis()
        private set
    var deathSequenceDuration: Int = MathUtils.random(minimumExplosions, maximumExplosions)
        private set

    fun incrementDeathSequence() {
        deathSequenceDuration--
        deathSequenceNextExplosion = TimeUtils.millis() + MathUtils.random(250, 750)
    }

}
