package com.gadarts.returnfire.utils

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btBoxShape
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject.CollisionFlags
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.shared.GameAssetManager

class CharacterPhysicsInitializer {
    fun initialize(entityBuilder: EntityBuilder, character: Entity, assetsManager: GameAssetManager) {
        val characterDefinition = ComponentsMapper.character.get(character).definition
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
            if (characterDefinition.isUseSeparateTransformObjectForPhysics()) Matrix4(modelInstanceTransform) else modelInstanceTransform
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
            characterDefinition.getLinearDamping(),
            characterDefinition.getAngularDamping()
        )
        rigidBody.friction = characterDefinition.getFriction()
        rigidBody.angularFactor = characterDefinition.getAngularFactor(Vector3())
        rigidBody.linearFactor = characterDefinition.getLinearFactor(Vector3())
    }

}
