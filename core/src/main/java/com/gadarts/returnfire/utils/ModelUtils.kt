package com.gadarts.returnfire.utils

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btBoxShape
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.badlogic.gdx.physics.bullet.collision.btCompoundShape
import com.gadarts.returnfire.assets.utils.ModelCollisionShapeInfo
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.utils.GeneralUtils.auxVector1

object ModelUtils {
    fun buildShapeFromModelCollisionShapeInfo(modelCollisionShapeInfo: ModelCollisionShapeInfo): btCollisionShape {
        val compoundShape = btCompoundShape()

        modelCollisionShapeInfo.collisionShapes.forEach { info ->
            val halfExtents = Vector3(info.dimensions).scl(0.5f)
            val boxShape = btBoxShape(halfExtents)
            val transform = Matrix4().setToTranslation(info.center)
            compoundShape.addChildShape(transform, boxShape)
        }

        return compoundShape
    }

    fun getPositionOfModel(entity: Entity): Vector3 {
        return getPositionOfModel(entity, auxVector1)
    }

    fun getPositionOfModel(entity: Entity, output: Vector3): Vector3 {
        return ComponentsMapper.modelInstance.get(entity).gameModelInstance.modelInstance.transform.getTranslation(
            output
        )
    }

    fun createFlatMesh(
        builder: ModelBuilder,
        meshName: String,
        size: Float,
        texture: Texture?,
        offset: Float = 0F
    ) {
        val material =
            if (texture != null) Material(TextureAttribute.createDiffuse(texture)) else Material(
                TextureAttribute(
                    TextureAttribute.Diffuse
                )
            )
        material.set(BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA))
        val mbp = builder.part(
            meshName,
            GL20.GL_TRIANGLES,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal or VertexAttributes.Usage.TextureCoordinates).toLong(),
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
