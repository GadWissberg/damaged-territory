package com.gadarts.returnfire.components

import com.badlogic.gdx.math.collision.BoundingBox

class BoxCollisionComponent : GameComponent() {
    private val boundingBox = BoundingBox()

    override fun reset() {

    }

    fun init(box: BoundingBox) {
        boundingBox.set(box)
    }

    fun getBoundingBox(output: BoundingBox): BoundingBox {
        return output.set(boundingBox)
    }

    fun setBoundingBox(box: BoundingBox) {
        boundingBox.set(box)
    }
}
