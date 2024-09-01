package com.gadarts.returnfire

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttributes.Usage.*
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper

object GeneralUtils {

    private val auxVector = Vector3()

    fun calculateVolumeAccordingToPosition(entity: Entity, camera: PerspectiveCamera): Float {
        val transform =
            ComponentsMapper.modelInstance.get(entity).gameModelInstance.modelInstance.transform
        val position = transform.getTranslation(auxVector)
        var distance = MathUtils.clamp(camera.position.dst2(position), 15F, 64F)
        distance = 1 - MathUtils.norm(15F, 64F, distance)
        return distance
    }

    fun createFlatMesh(
        builder: ModelBuilder,
        meshName: String,
        size: Float,
        texture: Texture,
        offset: Float = 0F
    ) {
        val material = Material(TextureAttribute.createDiffuse(texture))
        material.set(BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA))
        val mbp = builder.part(
            meshName,
            GL20.GL_TRIANGLES,
            (Position or Normal or TextureCoordinates).toLong(),
            material
        )
        mbp.setUVRange(0F, 0F, 1F, 1F)
        mbp.rect(
            offset + -size, 0F, offset + size,
            offset + size, 0F, offset + size,
            offset + size, 0F, offset + -size,
            offset + -size, 0F, offset + -size,
            0F, size, 0F,
        )
    }

}
