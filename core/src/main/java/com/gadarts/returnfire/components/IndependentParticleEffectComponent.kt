package com.gadarts.returnfire.components

import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect

abstract class BaseParticleEffectComponent : GameComponent() {

    lateinit var effect: ParticleEffect
        private set

    override fun reset() {
    }

    open fun init(effect: ParticleEffect) {
        this.effect = effect
    }

}

class FollowerParticleEffectComponent : BaseParticleEffectComponent()

class IndependentParticleEffectComponent : BaseParticleEffectComponent()

