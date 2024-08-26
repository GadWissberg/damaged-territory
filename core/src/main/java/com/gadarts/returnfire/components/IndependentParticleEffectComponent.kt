package com.gadarts.returnfire.components

import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition

abstract class BaseParticleEffectComponent : GameComponent() {

    lateinit var definition: ParticleEffectDefinition
    lateinit var effect: ParticleEffect
        private set

    override fun reset() {
    }

    open fun init(effect: ParticleEffect, definition: ParticleEffectDefinition) {
        this.effect = effect
        this.definition = definition
    }

}

class FollowerParticleEffectComponent : BaseParticleEffectComponent()

class IndependentParticleEffectComponent : BaseParticleEffectComponent()

