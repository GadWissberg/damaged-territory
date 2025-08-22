package com.gadarts.returnfire.ecs.components.ai

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import com.gadarts.returnfire.ecs.systems.ai.AiTurretStatus

class AiTurretComponent : Component {
    var nextLookingAroundTime: Long = 0
    var target: Entity? = null
    private val lookingAroundDestination: Vector2 = Vector2()

    fun getDestination(output: Vector2): Vector2 {
        return output.set(lookingAroundDestination)
    }

    fun setDestination(vector: Vector2) {
        lookingAroundDestination.set(vector)
    }

    var state: AiTurretStatus = AiTurretStatus.LOOK_AROUND
}
