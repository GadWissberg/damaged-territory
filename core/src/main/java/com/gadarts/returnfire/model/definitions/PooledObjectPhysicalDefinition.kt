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
                val auxVector = GeneralUtils.auxVector1
                val halfExtents = boundingBox.getDimensions(auxVector).scl(0.5F)
                val shape = btBoxShape(halfExtents)
                return shape
            }

        },
    ),
    TANK_CANNON_BULLET(
        5F,
        object : ShapeCreator {
            override fun create(boundingBox: BoundingBox): btCollisionShape {
                val auxVector = GeneralUtils.auxVector1
                val halfExtents = boundingBox.getDimensions(auxVector).scl(0.5F)
                val shape = btBoxShape(halfExtents)
                return shape
            }

        },
    ),
    MISSILE(
        4F,
        object : ShapeCreator {
            override fun create(boundingBox: BoundingBox): btCollisionShape {
                val auxVector = GeneralUtils.auxVector1
                val halfExtents = boundingBox.getDimensions(auxVector).scl(0.5F)
                val shape = btBoxShape(halfExtents)
                return shape
            }
        },
    ),
    PALM_TREE_LEAF(
        0.06F,
        object : ShapeCreator {
            override fun create(boundingBox: BoundingBox): btCollisionShape {
                val auxVector = GeneralUtils.auxVector1
                val halfExtents = boundingBox.getDimensions(auxVector).scl(0.5F)
                val shape = btBoxShape(halfExtents)
                return shape
            }
        },
    ),
    PALM_TREE_PART(
        0.12F,
        object : ShapeCreator {
            override fun create(boundingBox: BoundingBox): btCollisionShape {
                val auxVector = GeneralUtils.auxVector1
                val halfExtents = boundingBox.getDimensions(auxVector).scl(0.5F)
                val shape = btBoxShape(halfExtents)
                return shape
            }
        },
    ),
    BUILDING_0_PART(
        8F,
        object : ShapeCreator {
            override fun create(boundingBox: BoundingBox): btCollisionShape {
                val shape = btBoxShape(GeneralUtils.auxVector1.set(0.25F, 0.25F, 0.065F))
                return shape
            }
        },
    ),
    ANTENNA_PART(
        0.12F,
        object : ShapeCreator {
            override fun create(boundingBox: BoundingBox): btCollisionShape {
                val shape = btBoxShape(GeneralUtils.auxVector1.set(0.065F, 0.03F, 0.03F))
                return shape
            }
        },
    ),
}
