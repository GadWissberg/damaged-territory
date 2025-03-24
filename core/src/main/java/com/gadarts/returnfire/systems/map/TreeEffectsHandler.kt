package com.gadarts.returnfire.systems.map

import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject.CollisionFlags
import com.gadarts.returnfire.components.ComponentsMapper

class TreeEffectsHandler(
    private val mapSystemRelatedEntities: MapSystemRelatedEntities,
    private val mapSystem: MapSystem
) {
    fun update() {
        val treeEntities = mapSystemRelatedEntities.treeEntities
        for (tree in treeEntities) {
            val physicsComponent = ComponentsMapper.physics.get(tree)
            val rigidBody = physicsComponent.rigidBody
            if (rigidBody.collisionFlags == CollisionFlags.CF_CHARACTER_OBJECT) {
                rigidBody.getWorldTransform(auxMatrix)
                auxMatrix.getRotation(auxQuat)
                val upVector = auxVector.set(0f, 1f, 0f)
                auxQuat.transform(upVector)
                if (upVector.y < 0.85f) {
                    mapSystem.destroyTree(tree, false)
                }
            }
        }
    }

    companion object {
        private val auxVector = Vector3()
        private val auxMatrix = Matrix4()
        private val auxQuat = Quaternion()
    }
}
