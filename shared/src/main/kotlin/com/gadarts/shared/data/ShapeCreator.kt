package com.gadarts.shared.data

import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.gadarts.shared.GameAssetManager

interface ShapeCreator {
    fun create(
        boundingBox: BoundingBox,
        assetsManager: GameAssetManager,
        gameModelInstanceInfo: ImmutableGameModelInstanceInfo,
    ): btCollisionShape

}
