package com.gadarts.returnfire.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.ecs.components.model.GameModelInstance

class ChildModelInstanceComponent(
    val gameModelInstance: GameModelInstance,
    val followParentRotation: Boolean,
    relativePosition: Vector3
) : Component {
    var visible: Boolean = true
    val relativePosition = Vector3()

    init {
        this.relativePosition.set(relativePosition)
    }
}
