package com.gadarts.returnfire.model.definitions

import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.physics.bullet.collision.btBoxShape
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.gadarts.returnfire.model.ShapeCreator
import com.gadarts.returnfire.utils.GeneralUtils

enum class PooledObjectPhysicalDefinition(
    val mass: Float,
    val shapeCreator: ShapeCreator,
) {
    BULLET_FLAT(
        0.035F,
        object : ShapeCreator {
            override fun create(boundingBox: BoundingBox): btCollisionShape {
                val auxVector = GeneralUtils.auxVector
                val halfExtents = boundingBox.getDimensions(auxVector).scl(0.5F)
                val shape = btBoxShape(halfExtents)
                return shape
            }

        },
    ),
    TANK_CANNON_BULLET(
        10F,
        object : ShapeCreator {
            override fun create(boundingBox: BoundingBox): btCollisionShape {
                val auxVector = GeneralUtils.auxVector
                val halfExtents = boundingBox.getDimensions(auxVector).scl(0.5F)
                val shape = btBoxShape(halfExtents)
                return shape
            }

        },
    ),
    MISSILE(
        8F,
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