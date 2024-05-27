package com.gadarts.returnfire.components

import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox


class GameModelInstance(
    val modelInstance: ModelInstance,
    boundingBox: BoundingBox? = null
) {
    var isSphere: Boolean = false
        private set
    private val boundingBox = BoundingBox()

    init {
        if (boundingBox != null) {
            this.boundingBox.set(boundingBox)
            isSphere = true
        }
    }

    fun getBoundingBox(auxBox: BoundingBox): BoundingBox {
        return auxBox.set(boundingBox)
    }

    fun calculateBoundingBox() {
        modelInstance.calculateBoundingBox(boundingBox)
        val position = modelInstance.transform.getTranslation(auxVector1)
        val dimensions = boundingBox.getDimensions(auxVector2).scl(0.5F)
        this.boundingBox.min.set(position).sub(dimensions)
        this.boundingBox.max.set(position).add(dimensions)
        this.boundingBox.update()
    }

    companion object {
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
    }
}
