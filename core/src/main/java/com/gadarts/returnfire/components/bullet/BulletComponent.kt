package com.gadarts.returnfire.components.bullet

import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.components.GameComponent

class BulletComponent : GameComponent() {
    lateinit var explosion: ParticleEffectDefinition
    var createdTime: Long = 0
        private set
    lateinit var behavior: BulletBehavior
        private set

    override fun reset() {

    }

    fun init(behavior: BulletBehavior, explosion: ParticleEffectDefinition) {
        this.behavior = behavior
        this.createdTime = TimeUtils.millis()
        this.explosion = explosion
    }

}
