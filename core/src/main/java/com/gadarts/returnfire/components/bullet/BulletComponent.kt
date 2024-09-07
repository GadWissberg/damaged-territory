package com.gadarts.returnfire.components.bullet

import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.components.GameComponent

class BulletComponent : GameComponent() {
    var friendly: Boolean = false
    var explosive: Boolean = false
    var explosion: ParticleEffectDefinition? = null
    var createdTime: Long = 0
        private set
    lateinit var behavior: BulletBehavior
        private set

    override fun reset() {

    }

    fun init(behavior: BulletBehavior, explosion: ParticleEffectDefinition?, explosive: Boolean, friendly: Boolean) {
        this.behavior = behavior
        this.createdTime = TimeUtils.millis()
        this.explosion = explosion
        this.explosive = explosive
        this.friendly = friendly
    }

}
