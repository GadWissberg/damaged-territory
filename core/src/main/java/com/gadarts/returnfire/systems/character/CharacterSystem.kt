package com.gadarts.returnfire.systems.character

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ArmComponent

interface CharacterSystem {
    fun createBullet(
        speed: Float,
        relativePosition: Vector3,
        radius: Float,
        explosion: ParticleEffect,
        spark: Entity
    )
    fun positionSpark(
        arm: ArmComponent,
        modelInstance: ModelInstance,
        relativePosition: Vector3
    ): Entity

}
