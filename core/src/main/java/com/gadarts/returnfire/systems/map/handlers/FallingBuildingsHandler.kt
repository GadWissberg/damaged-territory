package com.gadarts.returnfire.systems.map.handlers

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.Collision
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.map.MapSystem
import com.gadarts.returnfire.systems.physics.BulletEngineHandler

class FallingBuildingsHandler(private val mapSystem: MapSystem, private val gamePlayManagers: GamePlayManagers) {
    private val fallingBuildingsToRemove = ArrayList<Entity>()
    private val fallingBuildings = ArrayList<Entity>()
    fun collapseBuilding(
        entity: Entity,
        gameSessionData: GameSessionData,
        gamePlayManagers: GamePlayManagers
    ) {
        val rigidBody = ComponentsMapper.physics.get(entity).rigidBody
        val collisionWorld = gameSessionData.physicsData.collisionWorld
        rigidBody.clearForces()
        rigidBody.linearVelocity = Vector3.Zero
        rigidBody.angularVelocity = Vector3.Zero
        collisionWorld.removeRigidBody(rigidBody)
        rigidBody.collisionFlags = btCollisionObject.CollisionFlags.CF_NO_CONTACT_RESPONSE
        collisionWorld.addRigidBody(
            rigidBody,
            BulletEngineHandler.COLLISION_GROUP_GENERAL,
            0
        )
        rigidBody.gravity = auxVector1.set(0F, -0.5F, 0F)
        rigidBody.activationState = Collision.DISABLE_DEACTIVATION
        fallingBuildings.add(entity)
        gamePlayManagers.soundPlayer.play(
            gamePlayManagers.assetsManager.getAssetByDefinition(
                SoundDefinition.FALLING_BUILDING
            )
        )
    }

    fun updateFallingBuildings() {
        for (fallingBuilding in fallingBuildings) {
            val physicsComponent = ComponentsMapper.physics.get(fallingBuilding)
            if (physicsComponent.rigidBody.worldTransform.getTranslation(auxVector1).y < -ComponentsMapper.amb.get(
                    fallingBuilding
                ).def.collapseThreshold
            ) {
                fallingBuildingAnimationDone(fallingBuilding)
            }
        }
        for (fallingBuilding in fallingBuildingsToRemove) {
            fallingBuildings.remove(fallingBuilding)
        }
        fallingBuildingsToRemove.clear()
    }

    private fun fallingBuildingAnimationDone(fallingBuilding: Entity) {
        val corpse = ComponentsMapper.amb.get(fallingBuilding).def.corpse ?: return

        auxMatrix.set(ComponentsMapper.modelInstance.get(fallingBuilding).gameModelInstance.modelInstance.transform)
        mapSystem.destroyAmbObject(fallingBuilding)
        val gameModelInstance = gamePlayManagers.factories.gameModelInstanceFactory.createGameModelInstance(
            corpse
        )
        val position = auxMatrix.getTranslation(auxVector4)
        auxMatrix.setTranslation(position.x, 0F, position.z)
        val entityBuilder = gamePlayManagers.ecs.entityBuilder
        val destroyedBuilding = entityBuilder.begin().addModelInstanceComponent(
            gameModelInstance,
            Vector3.Zero,
            gamePlayManagers.assetsManager.getCachedBoundingBox(ModelDefinition.BUILDING_0_DESTROYED)
        ).finishAndAddToEngine()
        val transform = gameModelInstance.modelInstance.transform.set(auxMatrix)
        entityBuilder.addPhysicsComponentToEntity(
            destroyedBuilding,
            ModelDefinition.BUILDING_0_DESTROYED.physicalShapeCreator!!.create(),
            0F,
            btCollisionObject.CollisionFlags.CF_STATIC_OBJECT,
            transform
        )
        fallingBuildingsToRemove.add(fallingBuilding)
        val specialEffectsFactory = gamePlayManagers.factories.specialEffectsFactory
        val sideBias = 1.5F
        val height = 0.5F
        auxMatrix.getTranslation(position)
        specialEffectsFactory.generateExplosion(
            auxVector2.set(position).add(-sideBias, height, sideBias),
            true,
            playSound = false
        )
        specialEffectsFactory.generateExplosion(
            auxVector2.set(position).add(sideBias, height, sideBias),
            true,
            playSound = false
        )
        specialEffectsFactory.generateExplosion(
            auxVector2.set(position).add(sideBias, height, -sideBias),
            true,
            playSound = false
        )
        specialEffectsFactory.generateExplosion(
            auxVector2.set(position).add(-sideBias, height, -sideBias),
            true,
            playSound = false
        )
        specialEffectsFactory.generateExplosion(
            auxVector2.set(position).add(-sideBias, height, 0F),
            true,
            playSound = false
        )
        specialEffectsFactory.generateExplosion(
            auxVector2.set(position).add(sideBias, height, 0F),
            true,
            playSound = false
        )
        gamePlayManagers.soundPlayer.play(
            gamePlayManagers.assetsManager.getAssetByDefinition(SoundDefinition.EXPLOSION_HUGE),
            position
        )
        mapSystem.blowAmbToParts(
            position,
            ModelDefinition.BUILDING_0_PART,
            2, 3,
            0.5F,
            true,
            2F, 4F, 1.5F
        )
    }

    companion object {
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxVector4 = Vector3()
        private val auxMatrix = Matrix4()
    }

}
