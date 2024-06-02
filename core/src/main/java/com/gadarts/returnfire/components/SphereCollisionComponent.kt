package com.gadarts.returnfire.components

class SphereCollisionComponent : GameComponent() {
    private var radius: Float = 0.0f

    override fun reset() {

    }

    fun init(radius: Float) {
        this.radius = radius
    }

}
