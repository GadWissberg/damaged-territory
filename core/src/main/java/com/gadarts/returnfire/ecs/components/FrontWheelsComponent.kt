package com.gadarts.returnfire.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.ecs.components.model.GameModelInstance

class FrontWheelsComponent(
    val rightWheel: GameModelInstance,
    val leftWheel: GameModelInstance,
    relativeX: Float,
    relativeY: Float,
    relativeZ: Float
) : Component {
    var steeringSide: Int = 0
    var steeringRotation: Float = 0.0f
    val rightRelativeTransform: Matrix4 =
        Matrix4().rotate(Vector3.Y, -90F).setTranslation(relativeX, relativeY, relativeZ)
    val leftRelativeTransform: Matrix4 =
        Matrix4().rotate(Vector3.Y, 90F).setTranslation(relativeX, relativeY, -relativeZ)
}
