package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.model.GameModelInstance

class ChildModelInstanceComponent(val gameModelInstance: GameModelInstance, relativePosition: Vector3) : Component {
    var visible: Boolean = true
    val relativePosition = Vector3()

    init {
        this.relativePosition.set(relativePosition)
    }
}
