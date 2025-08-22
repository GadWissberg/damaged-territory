package com.gadarts.shared.data.definitions

import com.badlogic.gdx.math.collision.BoundingBox
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.SharedUtils
import com.gadarts.shared.data.ImmutableGameModelInstanceInfo
import com.gadarts.shared.data.ShapeCreator

enum class PooledObjectPhysicalDefinition(
    val mass: Float,
    val shapeCreator: ShapeCreator,
) {
    BULLET_FLAT(
        0.035F,
        object : ShapeCreator {
            override fun create(
                boundingBox: BoundingBox,
                assetsManager: GameAssetManager,
                gameModelInstanceInfo: ImmutableGameModelInstanceInfo,
            ): com.badlogic.gdx.physics.bullet.collision.btCollisionShape {
                val auxVector = SharedUtils.auxVector
                val halfExtents = boundingBox.getDimensions(auxVector).scl(0.5F)
                val shape = com.badlogic.gdx.physics.bullet.collision.btBoxShape(halfExtents)
                return shape
            }

        },
    ),
    TANK_CANNON_BULLET(
        5F,
        object : ShapeCreator {
            override fun create(
                boundingBox: BoundingBox,
                assetsManager: GameAssetManager,
                gameModelInstanceInfo: ImmutableGameModelInstanceInfo,
            ): com.badlogic.gdx.physics.bullet.collision.btCollisionShape {
                val halfExtents = boundingBox.getDimensions(SharedUtils.auxVector).scl(0.5F)
                val shape = com.badlogic.gdx.physics.bullet.collision.btBoxShape(halfExtents)
                return shape
            }

        },
    ),
    MISSILE(
        8F,
        object : ShapeCreator {
            override fun create(
                boundingBox: BoundingBox,
                assetsManager: GameAssetManager,
                gameModelInstanceInfo: ImmutableGameModelInstanceInfo,
            ): com.badlogic.gdx.physics.bullet.collision.btCollisionShape {
                val halfExtents = boundingBox.getDimensions(SharedUtils.auxVector).scl(0.5F)
                val shape = com.badlogic.gdx.physics.bullet.collision.btBoxShape(halfExtents)
                return shape
            }
        },
    ),
    PALM_TREE_LEAF(
        0.06F,
        object : ShapeCreator {
            override fun create(
                boundingBox: BoundingBox,
                assetsManager: GameAssetManager,
                gameModelInstanceInfo: ImmutableGameModelInstanceInfo,
            ): com.badlogic.gdx.physics.bullet.collision.btCollisionShape {
                val halfExtents = boundingBox.getDimensions(SharedUtils.auxVector).scl(0.5F)
                val shape = com.badlogic.gdx.physics.bullet.collision.btBoxShape(halfExtents)
                return shape
            }
        },
    ),
    PALM_TREE_PART(
        0.12F,
        object : ShapeCreator {
            override fun create(
                boundingBox: BoundingBox,
                assetsManager: GameAssetManager,
                gameModelInstanceInfo: ImmutableGameModelInstanceInfo,
            ): com.badlogic.gdx.physics.bullet.collision.btCollisionShape {
                val halfExtents = boundingBox.getDimensions(SharedUtils.auxVector).scl(0.5F)
                val shape = com.badlogic.gdx.physics.bullet.collision.btBoxShape(halfExtents)
                return shape
            }
        },
    ),
    BUILDING_0_PART(
        8F,
        object : ShapeCreator {
            override fun create(
                boundingBox: BoundingBox,
                assetsManager: GameAssetManager,
                gameModelInstanceInfo: ImmutableGameModelInstanceInfo,
            ): com.badlogic.gdx.physics.bullet.collision.btCollisionShape {
                val shape = com.badlogic.gdx.physics.bullet.collision.btBoxShape(
                    SharedUtils.auxVector.set(
                        0.25F,
                        0.25F,
                        0.065F
                    )
                )
                return shape
            }
        },
    ),
    ANTENNA_PART(
        0.12F,
        object : ShapeCreator {
            override fun create(
                boundingBox: BoundingBox,
                assetsManager: GameAssetManager,
                gameModelInstanceInfo: ImmutableGameModelInstanceInfo,
            ): com.badlogic.gdx.physics.bullet.collision.btCollisionShape {
                val shape = com.badlogic.gdx.physics.bullet.collision.btBoxShape(
                    SharedUtils.auxVector.set(
                        0.065F,
                        0.03F,
                        0.03F
                    )
                )
                return shape
            }
        },
    ),
    WATCH_TOWER_DESTROYED_PART(
        2F,
        object : ShapeCreator {
            override fun create(
                boundingBox: BoundingBox,
                assetsManager: GameAssetManager,
                gameModelInstanceInfo: ImmutableGameModelInstanceInfo,
            ): com.badlogic.gdx.physics.bullet.collision.btCollisionShape {
                return SharedUtils.buildShapeFromModelCollisionShapeInfo(
                    assetsManager.getCachedModelCollisionShapeInfo(
                        gameModelInstanceInfo
                    )!!
                )
            }
        },
    ),
}
