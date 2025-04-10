package com.gadarts.returnfire.systems.map.react

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.Collision
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject.CollisionFlags
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition.*
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.amb.AmbComponent
import com.gadarts.returnfire.components.amb.AmbCorpsePart
import com.gadarts.returnfire.components.bullet.BulletComponent
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.components.physics.RigidBody
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.model.definitions.AmbDefinition
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.bullet.BulletSystem.Companion.auxBoundingBox
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.data.PhysicsCollisionEventData
import com.gadarts.returnfire.systems.map.MapSystem
import com.gadarts.returnfire.utils.GeneralUtils

class MapSystemOnPhysicsCollision(private val mapSystem: MapSystem) : HandlerOnEvent {
    override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        val entity0 = PhysicsCollisionEventData.colObj0.userData as Entity
        val entity1 = PhysicsCollisionEventData.colObj1.userData as Entity
        handleCollisionRoadWithBullets(
            entity0,
            entity1, gamePlayManagers
        ) || handleCollisionRoadWithBullets(
            entity1,
            entity0,
            gamePlayManagers
        ) || handleCollisionGroundWithHeavyStuffOnHighSpeed(
            entity0,
            entity1, gamePlayManagers
        ) || handleCollisionGroundWithHeavyStuffOnHighSpeed(
            entity1,
            entity0,
            gamePlayManagers
        ) || handleCollisionDestroyableAmbWithFastAndHeavyStuff(
            entity0,
            entity1,
            gameSessionData,
            gamePlayManagers
        ) || handleCollisionDestroyableAmbWithFastAndHeavyStuff(
            entity1,
            entity0,
            gameSessionData,
            gamePlayManagers
        )
    }

    private fun handleCollisionDestroyableAmbWithFastAndHeavyStuff(
        entity0: Entity,
        entity1: Entity,
        gameSessionData: GameSessionData,
        gamePlayManagers: GamePlayManagers
    ): Boolean {
        val ambComponent = ComponentsMapper.amb.get(entity0) ?: return false

        if (ambComponent.def.hp > 0 && ambComponent.hp > 0) {
            val otherRigidBody = ComponentsMapper.physics.get(entity1).rigidBody
            val bulletComponent = ComponentsMapper.bullet.get(entity1)
            if (!ambComponent.def.destroyedByExplosiveOnly) {
                destroyLightweightAmb(
                    otherRigidBody,
                    bulletComponent,
                    entity0,
                    ambComponent,
                    gameSessionData,
                    gamePlayManagers
                )
            } else if (bulletComponent != null && bulletComponent.explosive) {
                handleCollisionHealthyAmbWithFastAndHeavyStuff(entity0, entity1, gameSessionData, gamePlayManagers)
            }
        }
        return true
    }

    private fun handleCollisionRoadWithBullets(
        entity0: Entity,
        entity1: Entity,
        gamePlayManagers: GamePlayManagers
    ): Boolean {
        val roadComponent = ComponentsMapper.road.get(entity0)
        val bulletComponent = ComponentsMapper.bullet.get(entity1)
        if (roadComponent != null && bulletComponent != null) {
            if (bulletComponent.explosive) {
                handleRoadHit(entity0, gamePlayManagers)
            }
            return true
        }
        return false
    }

    private fun createFlyingPartsForAmb(
        entity: Entity,
        relativeOffset: Vector3,
        gamePlayManagers: GamePlayManagers
    ) {
        val specialEffectsFactory = gamePlayManagers.factories.specialEffectsFactory
        val ambComponent = ComponentsMapper.amb.get(entity)
        if (ambComponent?.def?.flyingPart != null) {
            specialEffectsFactory.generateFlyingParts(
                entity,
                ambComponent.def.flyingPart,
                min = ambComponent.def.minFlyingParts,
                max = ambComponent.def.maxFlyingParts,
                minForce = ambComponent.def.flyingPartMinImpulse,
                maxForce = ambComponent.def.flyingPartMaxImpulse,
                relativeOffset = relativeOffset
            )
        } else {
            specialEffectsFactory.generateFlyingParts(entity)
        }
    }

    private fun initiateAmbDestruction(
        amb: Entity,
        position: Vector3,
        gameSessionData: GameSessionData,
        gamePlayManagers: GamePlayManagers
    ) {
        val ambComponent = ComponentsMapper.amb.get(amb)
        val def = ambComponent.def
        val factories = gamePlayManagers.factories
        if (def.flyingPart != null) {
            factories.specialEffectsFactory.generateFlyingParts(
                character = amb,
                modelDefinition = def.flyingPart,
                min = def.minFlyingParts,
                max = def.minFlyingParts,
                mass = 0.5F
            )
        }
        createIndependentParticleEffect(position, SMOKE, gameSessionData, gamePlayManagers)
        gamePlayManagers.soundPlayer.play(
            gamePlayManagers.assetsManager.getAssetByDefinition(SoundDefinition.EXPLOSION),
            position
        )
        if (def.destructionSound != null) {
            gamePlayManagers.soundPlayer.play(
                gamePlayManagers.assetsManager.getAssetByDefinition(def.destructionSound),
                position
            )
        }
        if (def.hasDeathSequence) {
            gamePlayManagers.ecs.entityBuilder.addDeathSequenceComponentToEntity(amb, true, 6, 9)
            createIndependentParticleEffect(
                position = position.add(0F, 0F, 0F),
                particleEffectDefinition = SMOKE_BIG_RECTANGLE,
                gameSessionData = gameSessionData,
                gamePlayManagers = gamePlayManagers,
                ttlInSeconds = 0
            )
        } else {
            when (def) {
                AmbDefinition.ANTENNA -> {
                    initiateAntennaDestruction(amb, gamePlayManagers)
                }

                AmbDefinition.ROCK_BIG -> {
                    initiateRockBigDestruction(amb, gamePlayManagers)
                }

                AmbDefinition.WATCH_TOWER -> {
                    initiateWatchTowerDestruction(amb, gamePlayManagers)
                }

                else -> {}
            }
            mapSystem.destroyAmbObject(amb)
        }
    }

    private fun createSmokeForBuilding(
        ambComponent: AmbComponent,
        isBuilding: Boolean,
        beforeHp: Int,
        newHp: Int,
        gameModelInstance: GameModelInstance,
        gameSessionData: GameSessionData,
        gamePlayManagers: GamePlayManagers,
    ) {
        val half = ambComponent.def.hp / 2F
        if (isBuilding && beforeHp >= half && newHp < half) {
            gameModelInstance.getBoundingBox(auxBoundingBox)
            val particleEffectDefinition = SMOKE_LOOP_BIG
            createIndependentParticleEffect(
                GeneralUtils.getRandomPositionOnBoundingBox(auxBoundingBox, 0F),
                particleEffectDefinition,
                gameSessionData, gamePlayManagers,
                0
            )
            createIndependentParticleEffect(
                GeneralUtils.getRandomPositionOnBoundingBox(auxBoundingBox, 0F),
                particleEffectDefinition, gameSessionData, gamePlayManagers, 0
            )
            createIndependentParticleEffect(
                GeneralUtils.getRandomPositionOnBoundingBox(auxBoundingBox, 0F),
                particleEffectDefinition,
                gameSessionData, gamePlayManagers, 0
            )
            createIndependentParticleEffect(
                GeneralUtils.getRandomPositionOnBoundingBox(auxBoundingBox, 0F),
                particleEffectDefinition, gameSessionData, gamePlayManagers, 0
            )
        }
    }

    private fun applyAmbCorpsePartDestructionOnGroundImpact(
        ambCorpsePart: AmbCorpsePart,
        rigidBody: RigidBody,
        entity: Entity,
        gamePlayManagers: GamePlayManagers
    ) {
        if (ambCorpsePart.destroyOnGroundImpact && !(entity.isRemoving || entity.isScheduledForRemoval)) {
            val specialEffectsFactory = gamePlayManagers.factories.specialEffectsFactory
            specialEffectsFactory.generateExplosion(
                rigidBody.worldTransform.getTranslation(auxVector1), blastRing = true, playSound = true
            )
            specialEffectsFactory.generateExplosion(
                rigidBody.worldTransform.getTranslation(auxVector1),
                addBiasToPosition = true,
                blastRing = false,
                playSound = false
            )
            specialEffectsFactory.generateExplosion(
                rigidBody.worldTransform.getTranslation(auxVector1),
                addBiasToPosition = true,
                blastRing = false,
                playSound = false
            )
            specialEffectsFactory.generateFlyingParts(entity, ModelDefinition.WATCH_TOWER_DESTROYED_PART, 2, 3, 3F)
            gamePlayManagers.ecs.engine.removeEntity(entity)
        }
    }

    private fun handleRoadHit(
        roadEntity: Entity,
        gamePlayManagers: GamePlayManagers,
        forceDestruction: Boolean = false
    ) {
        val roadComponent = ComponentsMapper.road.get(roadEntity)
        if (roadComponent.hp > 0) {
            roadComponent.takeDamage()
            if (roadComponent.hp <= 0 || forceDestruction) {
                val modelInstance = ComponentsMapper.modelInstance.get(roadEntity).gameModelInstance.modelInstance
                val position =
                    modelInstance.transform.getTranslation(
                        auxVector1
                    )
                val assetsManager = gamePlayManagers.assetsManager
                gamePlayManagers.soundPlayer.play(
                    assetsManager.getAssetByDefinition(SoundDefinition.EXPLOSION_SMALL),
                    position
                )
                gamePlayManagers.factories.specialEffectsFactory.generateSmallFlyingParts(position.add(0F, 0.2F, 0F))
                val attribute = modelInstance.materials.get(0).get(TextureAttribute.Diffuse) as TextureAttribute
                val name = "${roadComponent.textureDefinition.fileName}_dead"
                gamePlayManagers.assetsManager.getTexturesDefinitions().definitions[name]?.let {
                    attribute.textureDescription.texture =
                        assetsManager.getTexture(it, MathUtils.random(1, it.frames) - 1)
                }
            }
        }
    }

    private fun initiateWatchTowerDestruction(amb: Entity, gamePlayManagers: GamePlayManagers) {
        gamePlayManagers.factories.specialEffectsFactory.generateFlyingParts(
            character = amb,
            modelDefinition = ModelDefinition.ROCK_PART_BIG,
            min = 2,
            max = 3,
            mass = 3F,
            minForce = 8F,
            maxForce = 14F
        )
        createAmbCorpsePart(
            ModelDefinition.WATCH_TOWER_DESTROYED,
            amb,
            auxVector1.set(
                0F,
                gamePlayManagers.assetsManager.getCachedBoundingBox(ModelDefinition.WATCH_TOWER).height / 2F,
                0F
            ),
            6F,
            12F,
            gamePlayManagers
        )
    }

    private fun initiateRockBigDestruction(amb: Entity, gamePlayManagers: GamePlayManagers) {
        gamePlayManagers.factories.specialEffectsFactory.generateFlyingParts(
            character = amb,
            modelDefinition = ModelDefinition.ROCK_PART_BIG,
            min = 2,
            max = 3,
            mass = 3F,
            minForce = 10F,
            maxForce = 16F
        )
    }

    private fun initiateAntennaDestruction(amb: Entity, gamePlayManagers: GamePlayManagers) {
        createAntennaBase(amb, gamePlayManagers)
        createAmbCorpsePart(
            ModelDefinition.ANTENNA_DESTROYED_BODY,
            amb,
            auxVector1.setZero(),
            1.5F,
            7F,
            gamePlayManagers
        )
    }

    private fun createAntennaBase(amb: Entity, gamePlayManagers: GamePlayManagers) {
        auxMatrix.set(ComponentsMapper.modelInstance.get(amb).gameModelInstance.modelInstance.transform)
        val baseGameModelInstance =
            gamePlayManagers.factories.gameModelInstanceFactory.createGameModelInstance(ModelDefinition.ANTENNA_DESTROYED_BASE)
        gamePlayManagers.ecs.entityBuilder.begin()
            .addModelInstanceComponent(
                baseGameModelInstance,
                Vector3.Zero,
                gamePlayManagers.assetsManager.getCachedBoundingBox(ModelDefinition.ANTENNA_DESTROYED_BASE)
            ).addPhysicsComponent(
                ModelDefinition.ANTENNA_DESTROYED_BASE.physicalShapeCreator!!.create(),
                CollisionFlags.CF_STATIC_OBJECT,
                baseGameModelInstance.modelInstance.transform.set(auxMatrix),
                1F,
                0F
            ).finishAndAddToEngine()
    }

    private fun createIndependentParticleEffect(
        position: Vector3,
        particleEffectDefinition: ParticleEffectDefinition,
        gameSessionData: GameSessionData,
        gamePlayManagers: GamePlayManagers,
        ttlInSeconds: Int = MathUtils.random(10, 20)
    ) {
        gamePlayManagers.ecs.entityBuilder.begin().addParticleEffectComponent(
            position,
            gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(
                particleEffectDefinition
            ), ttlInSeconds = ttlInSeconds
        ).finishAndAddToEngine()
    }

    private fun createAmbCorpsePart(
        modelDefinition: ModelDefinition,
        amb: Entity,
        relativePosition: Vector3,
        impulse: Float,
        mass: Float,
        gamePlayManagers: GamePlayManagers
    ) {
        auxMatrix.set(ComponentsMapper.modelInstance.get(amb).gameModelInstance.modelInstance.transform)
        val bodyGameModelInstance =
            gamePlayManagers.factories.gameModelInstanceFactory.createGameModelInstance(modelDefinition)
        val cachedBoundingBox =
            gamePlayManagers.assetsManager.getCachedBoundingBox(modelDefinition)
        val def = ComponentsMapper.amb.get(amb).def
        val ambCorpsePart = gamePlayManagers.ecs.entityBuilder.begin()
            .addModelInstanceComponent(
                bodyGameModelInstance,
                Vector3.Zero,
                cachedBoundingBox
            ).addAmbCorpsePart(def.corpsePartDestroyOnGroundImpact, def.corpsePartCollisionSound)
            .addPhysicsComponent(
                modelDefinition.physicalShapeCreator!!.create(),
                CollisionFlags.CF_CHARACTER_OBJECT,
                bodyGameModelInstance.modelInstance.transform.set(auxMatrix).trn(
                    0F,
                    cachedBoundingBox.height / 2F,
                    0F
                ).trn(relativePosition),
                0.5F,
                mass
            ).finishAndAddToEngine()
        ComponentsMapper.physics.get(ambCorpsePart).rigidBody.applyImpulse(
            auxVector1.setToRandomDirection().scl(impulse), auxVector2.setToRandomDirection()
        )

    }

    private fun handleCollisionGroundWithHeavyStuffOnHighSpeed(
        entity0: Entity,
        entity1: Entity,
        gamePlayManagers: GamePlayManagers
    ): Boolean {
        val roadComponent = ComponentsMapper.road.get(entity0)
        val physicsComponent = ComponentsMapper.physics.get(entity1)
        val result = false
        val ambCorpsePart = ComponentsMapper.ambCorpsePart.get(entity1)
        if (ComponentsMapper.ground.has(entity0) && ambCorpsePart != null) {
            val rigidBody = physicsComponent.rigidBody
            if (rigidBody.linearVelocity.len2() > 4 && rigidBody.mass > 5 || ambCorpsePart.destroyOnGroundImpact) {
                if (ambCorpsePart.collisionSound != null) {
                    gamePlayManagers.soundPlayer.play(
                        gamePlayManagers.assetsManager.getAssetByDefinition(ambCorpsePart.collisionSound),
                        rigidBody.worldTransform.getTranslation(auxVector1)
                    )
                }
                applyAmbCorpsePartDestructionOnGroundImpact(ambCorpsePart, rigidBody, entity1, gamePlayManagers)
            }
            return result
        }
        if (roadComponent != null && physicsComponent != null) {
            val rigidBody = physicsComponent.rigidBody
            if (rigidBody.linearVelocity.len2() > 4 && rigidBody.mass > 5) {
                handleRoadHit(entity0, gamePlayManagers, true)
            }
            return result
        }
        return result
    }

    private fun destroyLightweightAmb(
        otherRigidBody: RigidBody,
        bulletComponent: BulletComponent?,
        affectedEntity: Entity,
        ambComponent: AmbComponent,
        gameSessionData: GameSessionData,
        gamePlayManagers: GamePlayManagers
    ) {
        val otherSpeed = otherRigidBody.linearVelocity.len2()
        val isExplosive = bulletComponent != null && bulletComponent.explosive
        val position =
            ComponentsMapper.modelInstance.get(affectedEntity).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector1
            )
        if (otherSpeed > 10F || (otherSpeed > 0.1F && otherRigidBody.mass > 7)) {
            ambComponent.hp = 0
            if (bulletComponent != null) {
                if (isExplosive) {
                    if (MathUtils.random() >= 0.5F) {
                        createIndependentParticleEffect(position, FIRE_LOOP_SMALL, gameSessionData, gamePlayManagers)
                    }
                } else {
                    createIndependentParticleEffect(position, SMOKE, gameSessionData, gamePlayManagers)
                }
                if (ambComponent.def == AmbDefinition.PALM_TREE) {
                    mapSystem.destroyTree(affectedEntity, isExplosive)
                }
            }
            if ((ambComponent.def.stayOnDeath)) {
                val rigidBody = ComponentsMapper.physics.get(affectedEntity).rigidBody
                rigidBody.activationState = Collision.DISABLE_DEACTIVATION
                rigidBody.collisionFlags = CollisionFlags.CF_CHARACTER_OBJECT
                if (ambComponent.def == AmbDefinition.STREET_LIGHT) {
                    gamePlayManagers.ecs.entityBuilder.begin().addParticleEffectComponent(
                        position,
                        gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(
                            SPARK_MED
                        )
                    ).finishAndAddToEngine()
                }
            } else {
                mapSystem.destroyAmbObject(affectedEntity)
                createFlyingPartsForAmb(affectedEntity, auxVector3.set(0F, 0.5F, 0F), gamePlayManagers)
            }
        }
    }

    private fun handleCollisionHealthyAmbWithFastAndHeavyStuff(
        amb: Entity,
        otherCollider: Entity,
        gameSessionData: GameSessionData,
        gamePlayManagers: GamePlayManagers
    ) {
        val ambComponent = ComponentsMapper.amb.get(amb)
        val gameModelInstance = ComponentsMapper.modelInstance.get(amb).gameModelInstance
        val position =
            gameModelInstance.modelInstance.transform.getTranslation(
                auxVector1
            )
        val beforeHp = ambComponent.hp
        ambComponent.hp -= MathUtils.random(1, 2)
        val newHp = ambComponent.hp
        createFlyingPartsForAmb(otherCollider, Vector3.Zero, gamePlayManagers)
        if (newHp <= 0) {
            initiateAmbDestruction(amb, position, gameSessionData, gamePlayManagers)
        } else {
            createSmokeForBuilding(
                ambComponent,
                ambComponent.def == AmbDefinition.BUILDING_0,
                beforeHp,
                newHp,
                gameModelInstance,
                gameSessionData, gamePlayManagers
            )
        }
    }

    companion object {
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxVector3 = Vector3()
        private val auxMatrix = Matrix4()
    }
}
