package com.gadarts.returnfire.utils

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btBoxShape
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject.CollisionFlags
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.components.model.MutableGameModelInstanceInfo
import com.gadarts.returnfire.ecs.systems.EntityBuilder
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.SharedUtils
import com.gadarts.shared.assets.definitions.model.ModelDefinition
import com.gadarts.shared.data.type.CharacterType

class CharacterPhysicsInitializer(private val assetsManager: GameAssetManager) {
    fun initialize(entityBuilder: EntityBuilder, character: Entity) {
        val characterDefinition = ComponentsMapper.character.get(character).definition
        val modelCollisionShapeInfo = getModelCollisionShapeInfo(characterDefinition.getModelDefinition())
        val shape = if (modelCollisionShapeInfo != null) {
            SharedUtils.buildShapeFromModelCollisionShapeInfo(modelCollisionShapeInfo)
        } else btBoxShape(Vector3(0.5F, 0.15F, 0.35F))
        val modelInstanceComponent = ComponentsMapper.modelInstance.get(character)
        val modelInstanceTransform = modelInstanceComponent.gameModelInstance.modelInstance.transform
        val physicsTransform =
            if (characterDefinition.isUseSeparateTransformObjectForPhysics()) Matrix4(modelInstanceTransform) else modelInstanceTransform
        val physicsComponent = entityBuilder.addPhysicsComponentToEntity(
            character,
            shape,
            characterDefinition.getMass(),
            CollisionFlags.CF_CHARACTER_OBJECT,
            physicsTransform,
        )
        val rigidBody = physicsComponent.rigidBody
        rigidBody.gravity = characterDefinition.getGravity(Vector3())
        rigidBody.setDamping(characterDefinition.getLinearDamping(), characterDefinition.getAngularDamping())
        rigidBody.friction = characterDefinition.getFriction()
        rigidBody.angularFactor = characterDefinition.getAngularFactor(Vector3())
        rigidBody.linearFactor = characterDefinition.getLinearFactor(Vector3())
        applyPhysicsToTurret(character, entityBuilder)
    }

    private fun applyPhysicsToTurret(
        character: Entity,
        entityBuilder: EntityBuilder
    ) {
        val characterDefinition = ComponentsMapper.character.get(character).definition
        if (characterDefinition.getCharacterType() == CharacterType.TURRET) {
            val turret = ComponentsMapper.turretBase.get(character).turret
            if (turret != null) {
                val gameModelInstance = ComponentsMapper.modelInstance.get(turret).gameModelInstance
                val modelDefinition = gameModelInstance.gameModelInstanceInfo!!.modelDefinition!!
                val info =
                    getModelCollisionShapeInfo(modelDefinition)
                val turretShape = if (info != null) {
                    SharedUtils.buildShapeFromModelCollisionShapeInfo(info)
                } else {
                    btBoxShape(assetsManager.getCachedBoundingBox(modelDefinition).getDimensions(Vector3()).scl(0.3F))
                }
                entityBuilder.addPhysicsComponentToEntity(
                    turret,
                    turretShape,
                    1F,
                    CollisionFlags.CF_KINEMATIC_OBJECT,
                    gameModelInstance.modelInstance.transform,
                )
            }
        }
    }

    private fun getModelCollisionShapeInfo(
        modelDefinition: ModelDefinition
    ) = assetsManager.getCachedModelCollisionShapeInfo(
        auxGameModelInstanceInfo.set(
            modelDefinition,
            null
        )
    )

    companion object {
        private val auxGameModelInstanceInfo = MutableGameModelInstanceInfo()
    }
}
