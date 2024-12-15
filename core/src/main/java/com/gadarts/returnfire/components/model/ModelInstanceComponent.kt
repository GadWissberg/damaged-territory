package com.gadarts.returnfire.components.model

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.Pool

class ModelInstanceComponent : Component, Pool.Poolable {
    var hidden: Boolean = false
    var hideAt: Long = -1
    lateinit var gameModelInstance: GameModelInstance

    fun init(
        gameModelInstance: GameModelInstance,
        position: Vector3,
        boundingBox: BoundingBox?,
        direction: Float,
        hidden: Boolean,
        texture: Texture?
    ) {
        gameModelInstance.modelInstance.transform.idt()
        this.gameModelInstance = gameModelInstance
        val modelInstance = this.gameModelInstance.modelInstance
        modelInstance.transform.translate(position)
        modelInstance.transform.rotate(Vector3.Y, direction)
        if (texture != null) {
            val textureAttribute = modelInstance.materials.get(0)
                .get(TextureAttribute.Diffuse) as TextureAttribute
            textureAttribute.textureDescription.texture = texture
        }
        if (boundingBox == null) {
            this.gameModelInstance.calculateBoundingBox()
        } else {
            this.gameModelInstance.setBoundingSphere(boundingBox)
        }
        this.hidden = hidden
    }


    override fun reset() {
    }

}
