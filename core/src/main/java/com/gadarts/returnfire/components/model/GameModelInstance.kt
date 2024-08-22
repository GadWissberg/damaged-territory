package com.gadarts.returnfire.components.model

import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.gadarts.returnfire.assets.definitions.ModelDefinition


class GameModelInstance(
    val modelInstance: ModelInstance,
    val definition: ModelDefinition?,
    boundingBox: BoundingBox? = null,
    var sphere: Boolean = false
) {
    private val boundingBox = BoundingBox()

    init {
        if (boundingBox != null) {
            this.boundingBox.set(boundingBox)
            sphere = true
        }
    }

    fun getBoundingBox(auxBox: BoundingBox): BoundingBox {
        return auxBox.set(boundingBox)
    }

    override fun toString(): String {
        return definition.toString()
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
