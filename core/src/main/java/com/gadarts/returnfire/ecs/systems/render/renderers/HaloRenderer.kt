package com.gadarts.returnfire.ecs.systems.render.renderers

import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.ecs.components.model.HaloEffect

class HaloRenderer {
    private var time: Float = 0F


    fun render(
        position: Vector3,
        batch: ModelBatch,
        environment: Environment,
        haloEffect: HaloEffect,
        deltaTime: Float
    ) {
        time += deltaTime

        val haloModelInstance = haloEffect.haloModelInstance
        haloModelInstance.transform.setToTranslation(position.x, position.y + 0.3F, position.z)
        val blendingAttribute = haloModelInstance.materials.get(0).get(BlendingAttribute.Type) as BlendingAttribute
        val alpha = MathUtils.sin((time * 2f)) * 0.2f
        blendingAttribute.opacity = alpha

        val scale = 1.0f + MathUtils.sin((time * 4f)) * 0.2f
        haloModelInstance.transform.scl(scale, 1f, scale)

        batch.render(haloModelInstance, environment)
    }


}