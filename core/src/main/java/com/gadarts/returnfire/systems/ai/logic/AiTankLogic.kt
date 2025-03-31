package com.gadarts.returnfire.systems.ai.logic

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btPairCachingGhostObject
import com.gadarts.returnfire.components.AiComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.model.MapGraph
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.ai.AiStatus
import com.gadarts.returnfire.systems.ai.MapPathFinder
import com.gadarts.returnfire.systems.character.CharacterShootingHandler
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.player.handlers.movement.tank.TankMovementHandlerDesktop
import kotlin.math.abs
import kotlin.math.atan2


class AiTankLogic(
    private val pathFinder: MapPathFinder,
    private val mapGraph: MapGraph,
    entityBuilder: EntityBuilder,
    dispatcher: MessageDispatcher,
    private val gameSessionData: GameSessionData,
    autoAim: btPairCachingGhostObject,
) : AiCharacterLogic {
    private val movementHandler: TankMovementHandlerDesktop by lazy {
        val movementHandler = TankMovementHandlerDesktop()
        movementHandler
    }
    private val shootingHandler: CharacterShootingHandler by lazy {
        val handler = CharacterShootingHandler(entityBuilder)
        handler.initialize(dispatcher, gameSessionData, autoAim)
        handler
    }

    override fun preUpdate(character: Entity, deltaTime: Float) {
        val aiComponent = ComponentsMapper.ai.get(character)
        if (aiComponent.state == AiStatus.IDLE) {
            val position =
                ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector3_2
                )
            val start = mapGraph.getNode(position.x.toInt(), position.z.toInt())
            val transform =
                ComponentsMapper.modelInstance.get(gameSessionData.gamePlayData.player).gameModelInstance.modelInstance.transform
            val playerPosition =
                transform.getTranslation(
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
        val playerTransform =
            ComponentsMapper.modelInstance.get(gameSessionData.gamePlayData.player).gameModelInstance.modelInstance.transform
        val playerPosition = playerTransform.getTranslation(auxVector3_1)
        val characterPosition =
            ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector3_2
            )
        val turret = ComponentsMapper.turretBase.get(character).turret
        if (playerPosition.set(playerPosition.x, 0F, playerPosition.z)
                .dst2(characterPosition.set(characterPosition.x, 0F, characterPosition.z)) > 88F
        ) {
            shootingHandler.stopSecondaryShooting()
            shootingHandler.stopPrimaryShooting()
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
            shootingHandler: CharacterShootingHandler,
            character: Entity,
            deltaTime: Float,
            angleToTarget: Float,
            player: Entity
        ) {
            val facingDirection = getFacingDirection(character)
            movementHandler.applyTurretRotation(if (facingDirection > angleToTarget) 1 else -1, character)
        }

    }

    object TurretRotationLessThan180 : RotationCallback {
        override fun invoke(
            movementHandler: TankMovementHandlerDesktop,
            shootingHandler: CharacterShootingHandler,
            character: Entity,
            deltaTime: Float,
            angleToTarget: Float,
            player: Entity
        ) {
            val facingDirection = getFacingDirection(character)
            movementHandler.applyTurretRotation(if (facingDirection > angleToTarget) -1 else 1, character)
        }

    }

    object TurretRotationAnglesMatch : RotationCallback {
        override fun invoke(
            movementHandler: TankMovementHandlerDesktop,
            shootingHandler: CharacterShootingHandler,
            character: Entity,
            deltaTime: Float,
            angleToTarget: Float,
            player: Entity
        ) {
            movementHandler.applyTurretRotation(0, character)
            if (ComponentsMapper.character.get(player).definition.isFlyer()) {
                shootingHandler.startSecondaryShooting()
            } else {
                shootingHandler.startPrimaryShooting()
            }
        }

    }

    override fun update(character: Entity, deltaTime: Float) {
        movementHandler.update(character, deltaTime)
        shootingHandler.update(character)
    }

    private fun applyMovement(
        character: Entity,
        aiComponent: AiComponent,
        deltaTime: Float
    ) {
        val playerTransform =
            ComponentsMapper.modelInstance.get(gameSessionData.gamePlayData.player).gameModelInstance.modelInstance.transform
        val playerPosition =
            playerTransform.getTranslation(
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
            shootingHandler: CharacterShootingHandler,
            character: Entity,
            deltaTime: Float,
            angleToTarget: Float,
            player: Entity
        ) {
            val facingDirection = getFacingDirection(character)
            movementHandler.applyRotation(if (facingDirection > angleToTarget) -1 else 1, character)
            movementHandler.stopMovement()
        }

    }

    object BaseRotationGreaterThan180 : RotationCallback {
        override fun invoke(
            movementHandler: TankMovementHandlerDesktop,
            shootingHandler: CharacterShootingHandler,
            character: Entity,
            deltaTime: Float,
            angleToTarget: Float,
            player: Entity
        ) {
            val facingDirection = getFacingDirection(character)
            movementHandler.applyRotation(if (facingDirection > angleToTarget) 1 else -1, character)
            movementHandler.stopMovement()
        }

    }

    object BaseRotationAnglesMatch : RotationCallback {
        override fun invoke(
            movementHandler: TankMovementHandlerDesktop,
            shootingHandler: CharacterShootingHandler,
            character: Entity,
            deltaTime: Float,
            angleToTarget: Float,
            player: Entity
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
        if (gameSessionData.gamePlayData.player == null) return

        val transform = ComponentsMapper.modelInstance.get(entityToRotate).gameModelInstance.modelInstance.transform
        val position = transform.getTranslation(auxVector3_1)
        val facingDirection = getFacingDirection(entityToRotate)
        val directionToTarget = target.sub(position.x, position.z).nor()
        val angle = atan2(directionToTarget.y.toDouble(), directionToTarget.x.toDouble()).toFloat()
        val angleToTarget = (360 - (angle * MathUtils.radiansToDegrees)) % 360
        if (MathUtils.isEqual(facingDirection, angleToTarget, epsilon)) {
            rotationCallback.invoke(
                movementHandler,
                shootingHandler,
                entityToRotate,
                deltaTime,
                angleToTarget,
                gameSessionData.gamePlayData.player!!
            )
        } else {
            val angleDiff = abs(facingDirection - angleToTarget)
            val callback = if (angleDiff > 180) baseRotationGreaterThan180 else baseRotationLessThan180
            callback.invoke(
                movementHandler,
                shootingHandler,
                entityToRotate,
                deltaTime,
                angleToTarget,
                gameSessionData.gamePlayData.player!!
            )
        }
    }


    interface RotationCallback {
        fun invoke(
            movementHandler: TankMovementHandlerDesktop,
            shootingHandler: CharacterShootingHandler,
            character: Entity,
            deltaTime: Float,
            angleToTarget: Float,
            player: Entity
        )

    }

    companion object {
        private val auxVector3_1 = Vector3()
        private val auxVector3_2 = Vector3()
        private val auxVector2 = Vector2()
        private val auxQuaternion = Quaternion()
        private fun getFacingDirection(entity: Entity): Float {
            val transform = ComponentsMapper.modelInstance.get(entity).gameModelInstance.modelInstance.transform
            val rotationAroundY =
                transform.getRotation(auxQuaternion).yaw + if (transform.getRotation(auxQuaternion).yaw < 0) 360F else 0F
            return rotationAroundY
        }

    }
}
