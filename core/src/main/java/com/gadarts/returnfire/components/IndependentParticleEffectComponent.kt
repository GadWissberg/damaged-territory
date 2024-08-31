package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition

class ParticleEffectComponent : GameComponent() {

    var parent: Entity? = null
        private set
    lateinit var definition: ParticleEffectDefinition
    lateinit var effect: ParticleEffect
        private set

    override fun reset() {
    }

    fun init(effect: ParticleEffect, definition: ParticleEffectDefinition, parent: Entity?) {
        this.effect = effect
        this.definition = definition
        this.parent = parent
    }

}


