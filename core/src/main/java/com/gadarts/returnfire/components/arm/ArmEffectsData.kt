package com.gadarts.returnfire.components.arm

import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.systems.data.pools.GameParticleEffectPool

class ArmEffectsData(
    val explosion: ParticleEffectDefinition?,
    val smokeEmit: GameParticleEffectPool?,
    val sparkParticleEffect: GameParticleEffectPool,
    val smokeTrail: GameParticleEffectPool?,
)
