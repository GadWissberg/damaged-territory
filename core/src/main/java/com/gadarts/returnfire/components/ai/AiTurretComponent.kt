package com.gadarts.returnfire.components.ai

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.math.Vector2
import com.gadarts.returnfire.systems.ai.AiTurretStatus

class AiTurretComponent : Component {
    var nextLookingAroundTime: Long = 0
    private val destination: Vector2 = Vector2()
    fun getDestination(output: Vector2): Vector2 {
        return output.set(destination)
    }

    fun setDestination(vector: Vector2) {
        destination.set(vector)
    }

    var state: AiTurretStatus = AiTurretStatus.LOOK_AROUND
}
