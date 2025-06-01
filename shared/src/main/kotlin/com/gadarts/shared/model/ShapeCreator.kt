package com.gadarts.shared.model

import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape

interface ShapeCreator {
    fun create(boundingBox: BoundingBox): btCollisionShape

}
