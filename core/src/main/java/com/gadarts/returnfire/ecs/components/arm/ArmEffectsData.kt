package com.gadarts.returnfire.ecs.components.arm

import com.gadarts.returnfire.systems.data.pools.GameParticleEffectPool
import com.gadarts.shared.assets.definitions.ParticleEffectDefinition

class ArmEffectsData(
    val explosion: ParticleEffectDefinition?,
    val smokeEmit: GameParticleEffectPool?,
    val sparkParticleEffect: GameParticleEffectPool,
    val smokeTrail: GameParticleEffectPool?,
)
