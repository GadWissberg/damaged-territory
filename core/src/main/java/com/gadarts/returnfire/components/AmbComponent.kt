package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.model.AmbDefinition

class AmbComponent(val rotation: Float, val def: AmbDefinition, scale: Vector3) : Component {
    private val scale = Vector3()

    init {
        this.scale.set(scale)
    }

    fun getScale(output: Vector3): Vector3 {
        return output.set(scale)
    }

}
