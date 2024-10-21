package com.gadarts.returnfire.assets.definitions

import com.badlogic.gdx.physics.bullet.collision.btCollisionShape

interface PhysicalShapeCreator {
    fun create(): btCollisionShape
}
