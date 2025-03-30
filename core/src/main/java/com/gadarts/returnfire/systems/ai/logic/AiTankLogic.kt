package com.gadarts.returnfire.systems.ai.logic

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.AiComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.model.MapGraph
import com.gadarts.returnfire.systems.ai.AiStatus
import com.gadarts.returnfire.systems.ai.MapPathFinder
import com.gadarts.returnfire.systems.data.GameSessionDataGameplay
import com.gadarts.returnfire.systems.player.handlers.movement.tank.TankMovementHandlerDesktop
import kotlin.math.abs
import kotlin.math.atan2


class AiTankLogic(
    private val pathFinder: MapPathFinder,
    private val mapGraph: MapGraph,
    private val gamePlayData: GameSessionDataGameplay,
) : AiCharacterLogic {
    private val movementHandler: TankMovementHandlerDesktop by lazy {
        val movementHandler = TankMovementHandlerDesktop()
        movementHandler
    }


    override fun preUpdate(character: Entity, deltaTime: Float) {
        val aiComponent = ComponentsMapper.ai.get(character)
        if (aiComponent.state == AiStatus.IDLE) {
            val position =
                ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector3_2
                )
            val start = mapGraph.getNode(position.x.toInt(), position.z.toInt())
            val playerPosition =
                ComponentsMapper.modelInstance.get(gamePlayData.player).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector3_1
                )
            val end = mapGraph.getNode(playerPosition.x.toInt(), playerPosition.z.toInt())
            aiComponent.path.clear()
            val pathFound = pathFinder.searchNodePath(start, end, aiComponent.path)
            if (pathFound) {
                aiComponent.state = AiStatus.MOVING
                aiComponent.path.nodes.removeIndex(0)
            }
        } else if (aiComponent.state == AiStatus.MOVING) {
            applyMovement(character, aiComponent, deltaTime)
            aimAndShoot(character, deltaTime)
        }
    }

    private fun aimAndShoot(character: Entity, deltaTime: Float) {
        val playerPosition =
            ComponentsMapper.modelInstance.get(gamePlayData.player).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector3_1
            )
        val characterPosition =
            ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector3_2
            )
        val turret = ComponentsMapper.turretBase.get(character).turret
        if (playerPosition.set(playerPosition.x, 0F, playerPosition.z)
                .dst2(characterPosition.set(characterPosition.x, 0F, characterPosition.z)) > 88F
        ) {
            movementHandler.applyTurretRotation(0, turret)
            return
        }

        handleRotation(
            auxVector2.set(playerPosition.x, playerPosition.z),
            turret,
            deltaTime,
            TurretRotationAnglesMatch,
            TurretRotationGreaterThan180,
            TurretRotationLessThan180,
            4f
        )
    }

    object TurretRotationGreaterThan180 : RotationCallback {
        override fun invoke(
            movementHandler: TankMovementHandlerDesktop,
            character: Entity,
            deltaTime: Float,
            rotationAroundY: Float,
            angleToTarget: Float
        ) {
            movementHandler.applyTurretRotation(if (rotationAroundY > angleToTarget) 1 else -1, character)
        }

    }

    object TurretRotationLessThan180 : RotationCallback {
        override fun invoke(
            movementHandler: TankMovementHandlerDesktop,
            character: Entity,
            deltaTime: Float,
            rotationAroundY: Float,
            angleToTarget: Float
        ) {
            movementHandler.applyTurretRotation(if (rotationAroundY > angleToTarget) -1 else 1, character)
        }

    }

    object TurretRotationAnglesMatch : RotationCallback {
        override fun invoke(
            movementHandler: TankMovementHandlerDesktop,
            character: Entity,
            deltaTime: Float,
            rotationAroundY: Float,
            angleToTarget: Float
        ) {
            movementHandler.applyTurretRotation(0, character)
        }

    }

    override fun update(character: Entity, deltaTime: Float) {
        movementHandler.update(character, deltaTime)
    }

    private fun applyMovement(
        character: Entity,
        aiComponent: AiComponent,
        deltaTime: Float
    ) {
        val playerPosition =
            ComponentsMapper.modelInstance.get(gamePlayData.player).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector3_1
            )
        val pathNodes = aiComponent.path.nodes
        val destinationNode = pathNodes.get(pathNodes.size - 1)
        if (pathNodes.size == 0
            || playerPosition.dst2(
                auxVector3_2.set(
                    destinationNode.x.toFloat() + 0.5F,
                    0F,
                    destinationNode.y.toFloat() + 0.5F
                )
            ) > 15F
        ) {
            aiComponent.state = AiStatus.IDLE
            movementHandler.stopMovement()
            movementHandler.applyRotation(0, character)
            return
        }

        val transform = ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform
        val position =
            transform.getTranslation(
                auxVector3_1
            )
        val currentNode = mapGraph.getNode(position.x.toInt(), position.z.toInt())
        val nextNode = pathNodes.first()
        if (currentNode == nextNode) {
            pathNodes.removeIndex(0)
            if (pathNodes.size == 0) {
                aiComponent.state = AiStatus.IDLE
            }
        } else {
            handleRotation(
                auxVector2.set(nextNode.x.toFloat() + 0.5F, nextNode.y.toFloat() + 0.5F),
                character,
                deltaTime,
                BaseRotationAnglesMatch,
                BaseRotationGreaterThan180,
                BaseRotationLessThan180,
                32f
            )
        }
    }

    object BaseRotationLessThan180 : RotationCallback {
        override fun invoke(
            movementHandler: TankMovementHandlerDesktop,
            character: Entity,
            deltaTime: Float,
            rotationAroundY: Float,
            angleToTarget: Float
        ) {
            movementHandler.applyRotation(if (rotationAroundY > angleToTarget) -1 else 1, character)
            movementHandler.stopMovement()
        }

    }

    object BaseRotationGreaterThan180 : RotationCallback {
        override fun invoke(
            movementHandler: TankMovementHandlerDesktop,
            character: Entity,
            deltaTime: Float,
            rotationAroundY: Float,
            angleToTarget: Float
        ) {
            movementHandler.applyRotation(if (rotationAroundY > angleToTarget) 1 else -1, character)
            movementHandler.stopMovement()
        }

    }

    object BaseRotationAnglesMatch : RotationCallback {
        override fun invoke(
            movementHandler: TankMovementHandlerDesktop,
            character: Entity,
            deltaTime: Float,
            rotationAroundY: Float,
            angleToTarget: Float
        ) {
            movementHandler.applyRotation(0, character)
            movementHandler.thrust(character, deltaTime)
        }

    }

    private fun handleRotation(
        target: Vector2,
        entityToRotate: Entity,
        deltaTime: Float,
        rotationCallback: RotationCallback,
        baseRotationGreaterThan180: RotationCallback,
        baseRotationLessThan180: RotationCallback,
        epsilon: Float
    ) {
        val transform = ComponentsMapper.modelInstance.get(entityToRotate).gameModelInstance.modelInstance.transform
        val position =
            transform.getTranslation(
                auxVector3_1
            )
        val rotationAroundY =
            transform.getRotation(auxQuaternion).yaw + if (transform.getRotation(auxQuaternion).yaw < 0) 360F else 0F
        val directionToTarget = target.sub(position.x, position.z).nor()
        val angle = atan2(directionToTarget.y.toDouble(), directionToTarget.x.toDouble()).toFloat()
        val angleDegrees = angle * MathUtils.radiansToDegrees
        val angleToTarget = (360 - angleDegrees) % 360
        if (MathUtils.isEqual(rotationAroundY, angleToTarget, epsilon)) {
            rotationCallback.invoke(movementHandler, entityToRotate, deltaTime, rotationAroundY, angleToTarget)
        } else {
            val angleDiff = abs(rotationAroundY - angleToTarget)
            (if (angleDiff > 180) baseRotationGreaterThan180 else baseRotationLessThan180).invoke(
                movementHandler,
                entityToRotate,
                deltaTime,
                rotationAroundY,
                angleToTarget
            )
        }
    }

    interface RotationCallback {
        fun invoke(
            movementHandler: TankMovementHandlerDesktop,
            character: Entity,
            deltaTime: Float,
            rotationAroundY: Float,
            angleToTarget: Float
        )

    }

    companion object {
        private val auxVector3_1 = Vector3()
        private val auxVector3_2 = Vector3()
        private val auxVector2 = Vector2()
        private val auxQuaternion = Quaternion()
    }
}
