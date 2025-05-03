package com.gadarts.returnfire.systems.ai.logic

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btPairCachingGhostObject
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.ai.AiStatus
import com.gadarts.returnfire.systems.character.CharacterShootingHandler
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.player.handlers.movement.apache.ApacheMovementHandlerDesktop
import com.gadarts.returnfire.utils.ModelUtils

class AiApacheLogic(
    private val gameSessionData: GameSessionData,
    dispatcher: MessageDispatcher,
    entityBuilder: EntityBuilder,
    autoAim: btPairCachingGhostObject
) : AiCharacterLogic(dispatcher) {
    private var nextStrafeActivation: Long = 0
    private val shootingHandler = CharacterShootingHandler(entityBuilder)
    private val movementHandler: ApacheMovementHandlerDesktop by lazy {
        val movementHandler = ApacheMovementHandlerDesktop()
        movementHandler
    }

    init {
        shootingHandler.initialize(dispatcher, gameSessionData, autoAim)
    }

    override fun preUpdate(character: Entity, deltaTime: Float) {
        val aiComponent = ComponentsMapper.ai.get(character)
        val apacheAiComponent = ComponentsMapper.apacheAiComponent.get(character)
        val returningToBase = ComponentsMapper.hangarStage.has(aiComponent.target)
        val targetPosition = getPositionOfCurrentTarget(character)
        val characterTransform = ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform
        if (returningToBase) {
            movementHandler.thrust(character)
            onboard(character, 0.8F)
        } else if (!apacheAiComponent.runAway.isZero) {
            val characterPosition =
                ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector1
                )
            val distance = characterPosition.set(characterPosition.x, 0F, characterPosition.z)
                .dst2(auxVector5.set(apacheAiComponent.runAway.x, 0F, apacheAiComponent.runAway.z))
            if (distance < 10F) {
                apacheAiComponent.runAway.setZero()
            }
            movementHandler.thrust(character)
        } else {
            applyMainLogic(characterTransform, targetPosition, character)
        }
        handleRotation(targetPosition, characterTransform, character)
        update(character, deltaTime)
    }

    private fun getPositionOfCurrentTarget(
        character: Entity,
    ): Vector3 {
        val playerModelInstance =
            ComponentsMapper.modelInstance.get(gameSessionData.gamePlayData.player).gameModelInstance.modelInstance
        val aiComponent = ComponentsMapper.ai.get(character)
        val apacheAiComponent = ComponentsMapper.apacheAiComponent.get(character)
        val returningToBase = ComponentsMapper.hangarStage.has(aiComponent.target)
        return if (!returningToBase && apacheAiComponent.runAway.isZero) {
            playerModelInstance.transform.getTranslation(
                auxVector4
            )
        } else if (!apacheAiComponent.runAway.isZero) {
            auxVector4.set(apacheAiComponent.runAway)
        } else {
            ModelUtils.getPositionOfModel(aiComponent.target!!)
        }
    }

    private fun applyMainLogic(
        characterTransform: Matrix4,
        targetPosition: Vector3,
        character: Entity
    ) {
        val characterPosition = characterTransform.getTranslation(auxVector2)
        characterPosition.y = 0F
        targetPosition.y = 0F
        val distance = decideMovement(characterPosition, targetPosition, character)
        val player = gameSessionData.gamePlayData.player
        if (distance < MAX_DISTANCE_TO_THRUST_TO_TARGET
            && !GameDebugSettings.AI_ATTACK_DISABLED
            && player != null
            && !ComponentsMapper.character.get(
                player
            ).dead
        ) {
            shootingHandler.startPrimaryShooting()
            shootingHandler.startSecondaryShooting()
        } else {
            stopAttack()
        }
        val characterComponent = ComponentsMapper.character.get(character)
        val aiComponent = ComponentsMapper.ai.get(character)
        val apacheAiComponent = ComponentsMapper.apacheAiComponent.get(character)
        if (shouldReturnToBase(character)) {
            aiComponent.state = AiStatus.MOVING
            aiComponent.target = gameSessionData.mapData.stages[ComponentsMapper.boarding.get(
                character
            ).color]
            stopAttack()
        } else if (apacheAiComponent.runAway.isZero) {
            val lastHpCheck = apacheAiComponent.lastHpCheck
            if (characterComponent.hp <= lastHpCheck - characterComponent.definition.getHP() / 4F) {
                apacheAiComponent.lastHpCheck = characterComponent.hp
                val angle = MathUtils.random(0f, MathUtils.PI2)
                val x: Float = targetPosition.x + RUN_AWAY_RADIUS * MathUtils.cos(angle)
                val z: Float = targetPosition.z + RUN_AWAY_RADIUS * MathUtils.sin(angle)
                val tilesEntities = gameSessionData.mapData.tilesEntities
                apacheAiComponent.runAway.set(
                    MathUtils.clamp(x, 0F, tilesEntities[0].size - 1F),
                    0F,
                    MathUtils.clamp(z, 0F, tilesEntities.size - 1F)
                )
                stopAttack()
            }
        }
    }

    private fun stopAttack() {
        shootingHandler.stopPrimaryShooting()
        shootingHandler.stopSecondaryShooting()
    }

    private fun decideMovement(
        characterPosition: Vector3,
        targetPosition: Vector3?,
        character: Entity
    ): Float {
        val distance = characterPosition.dst2(targetPosition)
        if (distance > MAX_DISTANCE_TO_THRUST_TO_TARGET) {
            movementHandler.thrust(character)
        } else if (distance < 5F) {
            movementHandler.reverse()
        } else {
            movementHandler.stopMovement()
            decideStrafe()
        }
        return distance
    }

    private fun decideStrafe() {
        val now = TimeUtils.millis()
        if (!movementHandler.isStrafing()) {
            if (nextStrafeActivation == 0L) {
                decideNextStrafeActivation()
            } else if (nextStrafeActivation < now) {
                movementHandler.strafe(MathUtils.randomBoolean())
            }
        } else if (TimeUtils.timeSinceMillis(nextStrafeActivation) > 1000) {
            movementHandler.stopStrafe()
            decideNextStrafeActivation()
        }
    }

    private fun decideNextStrafeActivation() {
        nextStrafeActivation = MathUtils.random(2000, 6000) + TimeUtils.millis()
    }

    override fun update(character: Entity, deltaTime: Float) {
        movementHandler.update(
            character,
            deltaTime
        )
        shootingHandler.update(character)
    }

    private fun handleRotation(
        targetPosition: Vector3,
        characterTransform: Matrix4,
        character: Entity
    ) {
        val directionToTarget =
            auxVector3.set(targetPosition).sub(characterTransform.getTranslation(auxVector2)).nor()
        directionToTarget.y = 0F
        val characterDirection = auxVector2.set(Vector3.X)
        characterTransform.getRotation(auxQuat).transform(characterDirection).nor()
        characterDirection.y = 0F
        if (!directionToTarget.epsilonEquals(characterDirection, 0.4F)) {
            val crossProductY =
                characterDirection.x * directionToTarget.z - characterDirection.z * directionToTarget.x
            movementHandler.applyRotation(if (crossProductY > 0) -1 else 1, character)
        } else {
            movementHandler.applyRotation(0, character)
        }
    }

    companion object {
        const val MAX_DISTANCE_TO_THRUST_TO_TARGET = 14F
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxVector3 = Vector3()
        private val auxVector4 = Vector3()
        private val auxVector5 = Vector3()
        private val auxQuat = Quaternion()
        private const val RUN_AWAY_RADIUS = 15F
    }
}
