package com.gadarts.returnfire.systems.character

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ArmComponent
import com.gadarts.returnfire.components.arm.ArmProperties

interface CharacterSystem {
    fun createBullet(
        armProperties: ArmProperties,
        relativePosition: Vector3,
        spark: Entity,
    )
    fun positionSpark(
        arm: ArmComponent,
        modelInstance: ModelInstance,
        relativePosition: Vector3
    ): Entity

}
