package com.gadarts.returnfire.systems.ai

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
import com.gadarts.returnfire.systems.character.CharacterShootingHandler
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.player.handlers.movement.apache.ApacheMovementHandlerDesktop


class AiApacheLogic(
    private val gameSessionData: GameSessionData,
    private val dispatcher: MessageDispatcher,
    entityBuilder: EntityBuilder,
    autoAim: btPairCachingGhostObject
) {
    private var nextStrafeActivation: Long = 0
    private val shootingHandler = CharacterShootingHandler(entityBuilder)
    private val movementHandler: ApacheMovementHandlerDesktop by lazy {
        val movementHandler = ApacheMovementHandlerDesktop()
        movementHandler
    }

    init {
        shootingHandler.initialize(dispatcher, gameSessionData, autoAim)
    }

    fun updateCharacter(character: Entity, deltaTime: Float) {
        val boardingComponent = ComponentsMapper.boarding.get(character)
        if (boardingComponent != null && boardingComponent.isBoarding()) return

        val targetModelInstance =
            ComponentsMapper.modelInstance.get(gameSessionData.gamePlayData.player).gameModelInstance.modelInstance
        val aiComponent = ComponentsMapper.ai.get(character)
        val returnToBase = aiComponent.returnToBase
        val targetPosition =
            if (!returnToBase && aiComponent.runAway.isZero) targetModelInstance.transform.getTranslation(
                auxVector4
            ) else if (!aiComponent.runAway.isZero) {
                auxVector4.set(aiComponent.runAway)
            } else {
                ComponentsMapper.modelInstance.get(
                    gameSessionData.mapData.stages[boardingComponent.color]
                ).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector4
                )
            }
        val characterTransform = ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform
        if (returnToBase) {
            movementHandler.thrust(character)
            val characterPosition = characterTransform.getTranslation(auxVector2)
            if (characterPosition.epsilonEquals(targetPosition.x, characterPosition.y, targetPosition.z, 0.4F)) {
                dispatcher.dispatchMessage(
                    SystemEvents.CHARACTER_REQUEST_BOARDING.ordinal,
                    character
                )
            }
        } else if (!aiComponent.runAway.isZero) {
            val characterPosition =
                ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector1
                )
            val distance = characterPosition.set(characterPosition.x, 0F, characterPosition.z)
                .dst2(auxVector5.set(aiComponent.runAway.x, 0F, aiComponent.runAway.z))
            if (distance < 10F
            ) {
                aiComponent.runAway.setZero()
            }
            movementHandler.thrust(character)
        } else {
            applyMainLogic(characterTransform, targetPosition, character)
        }
        handleRotation(targetPosition, characterTransform)
        updateHandlers(character, deltaTime)
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
        if (distance < 9F && !GameDebugSettings.AI_ATTACK_DISABLED) {
            shootingHandler.startPrimaryShooting()
            shootingHandler.startSecondaryShooting()
        }
        val characterComponent = ComponentsMapper.character.get(character)
        val quarter = characterComponent.definition.getHP() / 4F
        val aiComponent = ComponentsMapper.ai.get(character)
        if (characterComponent.hp <= quarter) {
            aiComponent.returnToBase()
            stopAttack()
        } else if (aiComponent.runAway.isZero) {
            val lastHpCheck = aiComponent.lastHpCheck
            if (characterComponent.hp <= lastHpCheck - quarter) {
                aiComponent.lastHpCheck = characterComponent.hp
                val angle = MathUtils.random(0f, MathUtils.PI2)
                val x: Float = targetPosition.x + RUN_AWAY_RADIUS * MathUtils.cos(angle)
                val z: Float = targetPosition.z + RUN_AWAY_RADIUS * MathUtils.sin(angle)
                val tilesEntities = gameSessionData.mapData.tilesEntities
                aiComponent.runAway.set(
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
        if (distance > 14F) {
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

    private fun updateHandlers(character: Entity, deltaTime: Float) {
        movementHandler.update(
            character,
            deltaTime
        )
        shootingHandler.update(character)
    }

    private fun handleRotation(
        targetPosition: Vector3,
        characterTransform: Matrix4
    ) {
        val directionToTarget = auxVector3.set(targetPosition).sub(characterTransform.getTranslation(auxVector2)).nor()
        directionToTarget.y = 0F
        val characterDirection = auxVector2.set(Vector3.X)
        characterTransform.getRotation(auxQuat).transform(characterDirection).nor()
        characterDirection.y = 0F
        if (!directionToTarget.epsilonEquals(characterDirection, 0.4F)) {
            val crossProductY = characterDirection.x * directionToTarget.z - characterDirection.z * directionToTarget.x
            movementHandler.applyRotation(if (crossProductY > 0) -1 else 1)
        } else {
            movementHandler.applyRotation(0)
        }
    }

    companion object {
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxVector3 = Vector3()
        private val auxVector4 = Vector3()
        private val auxVector5 = Vector3()
        private val auxQuat = Quaternion()
        private const val RUN_AWAY_RADIUS = 15F
    }
}
