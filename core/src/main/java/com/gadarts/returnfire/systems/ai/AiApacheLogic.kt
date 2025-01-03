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
import com.gadarts.returnfire.systems.player.handlers.movement.apache.ApacheMovementHandlerDesktop

class AiApacheLogic(
    private val gameSessionData: GameSessionData,
    entityBuilder: EntityBuilder,
    dispatcher: MessageDispatcher,
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
        val targetPosition = targetModelInstance.transform.getTranslation(auxVector1)
        val characterTransform = ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform
        handleRotation(targetPosition, characterTransform)
        val characterPosition = characterTransform.getTranslation(auxVector2)
        characterPosition.y = 0F
        targetPosition.y = 0F
        val distance = decideMovement(characterPosition, targetPosition, character)
        if (distance < 9F && !GameDebugSettings.AI_ATTACK_DISABLED) {
            shootingHandler.startPrimaryShooting()
            shootingHandler.startSecondaryShooting()
        }
        updateHandlers(character, deltaTime)
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
        private val auxQuat = Quaternion()
    }
}
