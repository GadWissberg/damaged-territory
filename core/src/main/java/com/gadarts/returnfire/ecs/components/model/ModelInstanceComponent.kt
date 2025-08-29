package com.gadarts.returnfire.ecs.components.model

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.Pool

class ModelInstanceComponent : Component, Pool.Poolable {
    var outlineEffect: Boolean = false
        private set
    var hidden: Boolean = false
    var hideAt: Long = -1
    lateinit var gameModelInstance: GameModelInstance

    fun init(
        gameModelInstance: GameModelInstance,
        position: Vector3,
        boundingBox: BoundingBox?,
        direction: Float,
        hidden: Boolean,
        texture: Texture?,
        outlineEffect: Boolean = false
    ) {
        gameModelInstance.modelInstance.transform.idt()
        this.gameModelInstance = gameModelInstance
        val modelInstance = this.gameModelInstance.modelInstance
        modelInstance.transform.translate(position)
        modelInstance.transform.rotate(Vector3.Y, direction)
        if (texture != null) {
            val textureMat = modelInstance.materials.find { it.has(TextureAttribute.Diffuse) }
            if (textureMat != null) {
                val textureAttribute = textureMat.get(TextureAttribute.Diffuse) as TextureAttribute
                textureAttribute.textureDescription.texture = texture
            }
        }
        if (boundingBox == null) {
            this.gameModelInstance.calculateBoundingBox()
        } else {
            this.gameModelInstance.setBoundingSphere(boundingBox)
        }
        this.hidden = hidden
        this.outlineEffect = outlineEffect
    }


    override fun reset() {
    }

}
