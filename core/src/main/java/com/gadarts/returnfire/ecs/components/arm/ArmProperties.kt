package com.gadarts.returnfire.ecs.components.arm

import com.badlogic.gdx.audio.Sound
import com.gadarts.returnfire.systems.data.pools.RigidBodyPool

class ArmProperties(
    val damage: Float,
    val shootingSound: Sound,
    val reloadDuration: Long,
    val speed: Float,
    val effectsData: ArmEffectsData,
    val renderData: ArmRenderData,
    val explosive: Boolean,
    val rigidBodyPool: RigidBodyPool,
    val ammo: Int,
    val aimingRestriction: com.gadarts.returnfire.ecs.systems.character.factories.AimingRestriction? = null,
    val destroyOnSky: Boolean = true,
)
