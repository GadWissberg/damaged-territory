package com.gadarts.returnfire.factories

import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject
import com.badlogic.gdx.physics.bullet.collision.btCompoundShape
import com.badlogic.gdx.physics.bullet.collision.btConeShape
import com.badlogic.gdx.physics.bullet.collision.btPairCachingGhostObject
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.player.PlayerSystem

class AutoAimShapeFactory(private val gameSessionData: GameSessionData) {
    fun generate(collisionFilterGroup: Int, collisionFilterMask: Int): btPairCachingGhostObject {
        val ghostObject = btPairCachingGhostObject()
        ghostObject.collisionShape = btCompoundShape()
        (ghostObject.collisionShape as btCompoundShape).addChildShape(
            Matrix4(),
            btConeShape(AUTO_AIM_RADIUS, PlayerSystem.AUTO_AIM_HEIGHT)
        )
        (ghostObject.collisionShape as btCompoundShape).addChildShape(
            Matrix4().translate(1.5F, 2.5F, 0F).rotate(Vector3.Z, 45F),
            btConeShape(AUTO_AIM_RADIUS, PlayerSystem.AUTO_AIM_HEIGHT / 2F)
        )
        ghostObject.collisionFlags = btCollisionObject.CollisionFlags.CF_NO_CONTACT_RESPONSE
        gameSessionData.physicsData.collisionWorld.addCollisionObject(
            ghostObject,
            collisionFilterGroup,
            collisionFilterMask
        )
        return ghostObject
    }

    companion object {
        private const val AUTO_AIM_RADIUS: Float = 0.5F
    }
}
