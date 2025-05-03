package com.gadarts.returnfire.utils

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btBoxShape
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject.CollisionFlags
import com.badlogic.gdx.physics.bullet.collision.btCompoundShape
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.managers.GameAssetManager
import com.gadarts.returnfire.model.definitions.SimpleCharacterDefinition
import com.gadarts.returnfire.systems.EntityBuilder

class CharacterPhysicsInitializer {
    fun initialize(entityBuilder: EntityBuilder, character: Entity, assetsManager: GameAssetManager) {
        val characterDefinition = ComponentsMapper.character.get(character).definition
        val isApache = characterDefinition == SimpleCharacterDefinition.APACHE
        val modelCollisionShapeInfo =
            assetsManager.getCachedModelCollisionShapeInfo(characterDefinition.getModelDefinition())
        val shape = if (modelCollisionShapeInfo != null) {
            ModelUtils.buildShapeFromModelCollisionShapeInfo(
                modelCollisionShapeInfo
            )
        } else btBoxShape(
            Vector3(0.5F, 0.15F, 0.35F)
        )
        val modelInstanceComponent = ComponentsMapper.modelInstance.get(character)
        val modelInstanceTransform =
            modelInstanceComponent.gameModelInstance.modelInstance.transform
        val physicsTransform =
            if (isApache) Matrix4(modelInstanceTransform) else modelInstanceTransform
        val physicsComponent = entityBuilder.addPhysicsComponentToEntity(
            character,
            shape,
            10F,
            CollisionFlags.CF_CHARACTER_OBJECT,
            physicsTransform,
        )
        val rigidBody = physicsComponent.rigidBody
        rigidBody.gravity = characterDefinition.getGravity(Vector3())
        rigidBody.setDamping(
            if (isApache) 0F else 0.1F,
            if (isApache) 0.75F else 0.99F
        )
        if (!isApache) {
            rigidBody.friction = 0F
        }
        rigidBody.angularFactor = characterDefinition.getAngularFactor(Vector3())
        rigidBody.linearFactor = characterDefinition.getLinearFactor(Vector3())
    }

    private fun createApacheCollisionShape(): btCompoundShape {
        val shape = btCompoundShape()
        val bodyShape = btBoxShape(
            Vector3(0.4F, 0.08F, 0.1F)
        )
        val tailShape = btBoxShape(
            Vector3(0.3F, 0.04F, 0.04F)
        )
        val wing = btBoxShape(
            Vector3(0.1F, 0.05F, 0.1F)
        )
        shape.addChildShape(
            Matrix4().translate(Vector3(0.05F, -0.15F, 0F)).rotate(Vector3.Z, -15F), bodyShape
        )
        shape.addChildShape(
            Matrix4().translate(Vector3(-0.6F, 0F, 0F)).rotate(Vector3.Z, -15F), tailShape
        )
        shape.addChildShape(
            Matrix4().translate(Vector3(0F, -0.2F, 0.2F)).rotate(Vector3.Z, -10F), wing
        )
        shape.addChildShape(
            Matrix4().translate(Vector3(0F, -0.2F, -0.2F)).rotate(Vector3.Z, -10F), wing
        )
        return shape
    }

}
