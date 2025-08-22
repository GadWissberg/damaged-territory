package com.gadarts.returnfire.ecs.systems.character

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.arm.ArmComponent
import com.gadarts.returnfire.managers.GamePlayManagers

interface CharacterSystem {

    fun positionSpark(
        arm: ArmComponent,
        modelInstance: ModelInstance,
        relativePosition: Vector3
    ): Entity

    fun playAmbSound(entity: Entity, gamePlayManagers: GamePlayManagers)

}
