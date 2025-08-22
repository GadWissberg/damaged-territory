package com.gadarts.returnfire.ecs.components.model

import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.gadarts.shared.data.GameModelInstanceInfo

class GameModelInstance(
    val modelInstance: ModelInstance,
    val gameModelInstanceInfo: GameModelInstanceInfo,
    val shadow: ModelInstance? = null,
) {
    var sphere: Boolean = false
        private set

    init {
        if (shadow != null) {
            shadow.transform = modelInstance.transform
        }
    }

    private val boundingBox = BoundingBox()

    fun getBoundingBox(auxBox: BoundingBox): BoundingBox {
        return auxBox.set(boundingBox)
    }

    override fun toString(): String {
        return gameModelInstanceInfo.definition.toString()
    }

    fun calculateBoundingBox() {
        modelInstance.calculateBoundingBox(boundingBox)
        val position = modelInstance.transform.getTranslation(auxVector1)
        val dimensions = boundingBox.getDimensions(auxVector2).scl(0.5F)
        this.boundingBox.min.set(position).sub(dimensions)
        this.boundingBox.max.set(position).add(dimensions)
        this.boundingBox.update()
    }

    fun setBoundingSphere(boundingBox: BoundingBox) {
        this.boundingBox.set(boundingBox)
        this.sphere = true
    }

    fun setBoundingBox(boundingBox: BoundingBox) {
        this.boundingBox.set(boundingBox)
        this.sphere = false
    }

    companion object {
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
    }
}
