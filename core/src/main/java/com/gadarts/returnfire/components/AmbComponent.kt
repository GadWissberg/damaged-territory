package com.gadarts.returnfire.components

import com.badlogic.gdx.math.Vector3

class AmbComponent : GameComponent() {
    private val scale = Vector3()
    var rotation: Float = 0F

    override fun reset() {

    }

    fun getScale(output: Vector3): Vector3 {
        return output.set(scale)
    }

    fun init(scale: Vector3, rotation: Float) {
        this.scale.set(scale)
        this.rotation = rotation
    }

}
