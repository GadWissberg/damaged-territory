package com.gadarts.returnfire.factories

import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.badlogic.gdx.physics.bullet.collision.btPairCachingGhostObject

class GhostFactory {
    fun create(
        shape: btCollisionShape,
        position: Vector3,
    ): btPairCachingGhostObject {
        val ghostObject = btPairCachingGhostObject()
        ghostObject.collisionShape = shape
        ghostObject.collisionFlags = btCollisionObject.CollisionFlags.CF_NO_CONTACT_RESPONSE
        ghostObject.worldTransform = Matrix4().setToTranslation(position)
        return ghostObject
    }

}
