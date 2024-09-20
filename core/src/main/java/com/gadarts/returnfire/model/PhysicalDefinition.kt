package com.gadarts.returnfire.model

import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.physics.bullet.collision.btBoxShape
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.gadarts.returnfire.GeneralUtils

enum class PhysicalDefinition(
    val mass: Float,
    val shapeCreator: ShapeCreator,
) {
    BULLET(
        1F,
        object : ShapeCreator {
            override fun create(boundingBox: BoundingBox): btCollisionShape {
                val auxVector = GeneralUtils.auxVector
                val halfExtents = boundingBox.getDimensions(auxVector).scl(0.5F)
                val shape = btBoxShape(halfExtents)
                return shape
            }

        },
    );

}
