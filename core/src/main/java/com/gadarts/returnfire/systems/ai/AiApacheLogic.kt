package com.gadarts.returnfire.systems.ai

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.player.handlers.movement.VehicleMovementHandler
import com.gadarts.returnfire.systems.player.handlers.movement.apache.ApacheMovementHandlerDesktop

class AiApacheLogic(private val gameSessionData: GameSessionData) {
    private val movementHandler: VehicleMovementHandler by lazy {
        val movementHandler = ApacheMovementHandlerDesktop()
        movementHandler
    }

    fun updateCharacter(character: Entity, deltaTime: Float) {
        val boardingComponent = ComponentsMapper.boarding.get(character)
        if (boardingComponent != null && boardingComponent.isBoarding()) return

        val targetModelInstance =
            ComponentsMapper.modelInstance.get(gameSessionData.gamePlayData.player).gameModelInstance.modelInstance
        val targetPosition = targetModelInstance.transform.getTranslation(auxVector1)
        val characterTransform = ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform
        val directionToTarget = targetPosition.sub(
            characterTransform.getTranslation(
                auxVector2
            )
        ).nor()
        directionToTarget.y = 0F
        val characterDirection = auxVector2.set(Vector3.X)
        characterTransform.getRotation(auxQuat).transform(characterDirection).nor()
        characterDirection.y = 0F
        if (!directionToTarget.epsilonEquals(characterDirection, 0.1F)) {
            val crossProductY = characterDirection.x * directionToTarget.z - characterDirection.z * directionToTarget.x
            movementHandler.applyRotation(if (crossProductY > 0) 1 else -1)
        }
        movementHandler.update(
            character,
            deltaTime
        )
    }

    companion object {
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxQuat = Quaternion()
    }
}
