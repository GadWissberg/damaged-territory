package com.gadarts.returnfire.components.arm

import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect

class ArmProperties(
    val shootingSound: Sound,
    val reloadDuration: Long,
    val speed: Float,
    val radius: Float,
    val explosion: ParticleEffect? = null
)
