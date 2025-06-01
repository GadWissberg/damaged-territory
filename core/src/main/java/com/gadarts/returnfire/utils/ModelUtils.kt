package com.gadarts.returnfire.utils

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btBoxShape
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.badlogic.gdx.physics.bullet.collision.btCompoundShape
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.utils.GeneralUtils.auxVector1
import com.gadarts.shared.assets.utils.ModelCollisionShapeInfo

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


}
