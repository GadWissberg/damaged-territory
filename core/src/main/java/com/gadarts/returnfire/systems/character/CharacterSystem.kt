package com.gadarts.returnfire.systems.character

import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ArmComponent

interface CharacterSystem {
    fun createBullet(speed: Float, relativePosition: Vector3, radius: Float)
    fun positionSpark(
        arm: ArmComponent,
        modelInstance: ModelInstance,
        relativePosition: Vector3
    ): Decal

}
