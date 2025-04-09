package com.gadarts.returnfire.systems.ai.logic

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback
import com.badlogic.gdx.physics.bullet.collision.btPairCachingGhostObject
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.components.AiComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.ComponentsMapper.ai
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.model.MapGraph
import com.gadarts.returnfire.model.graph.MapGraphNode
import com.gadarts.returnfire.systems.ai.AiStatus
import com.gadarts.returnfire.systems.ai.logic.AiTankLogic.BaseRotationAnglesMatch.MAX_LOOKING_AHEAD
import com.gadarts.returnfire.systems.character.CharacterShootingHandler
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.physics.BulletEngineHandler.Companion.COLLISION_GROUP_AI
import com.gadarts.returnfire.systems.physics.BulletEngineHandler.Companion.COLLISION_GROUP_GENERAL
import com.gadarts.returnfire.systems.physics.BulletEngineHandler.Companion.COLLISION_GROUP_PLAYER
import com.gadarts.returnfire.systems.player.handlers.movement.tank.TankMovementHandlerDesktop
import com.gadarts.returnfire.utils.GeneralUtils
import kotlin.math.abs
import kotlin.math.atan2


class AiTankLogic(
    private val gameSessionData: GameSessionData,
    autoAim: btPairCachingGhostObject,
    private val gamePlayManagers: GamePlayManagers,
) : AiCharacterLogic, Disposable {
    private val movementHandler: TankMovementHandlerDesktop by lazy {
        val movementHandler = TankMovementHandlerDesktop()
        movementHandler
    }
    private val shootingHandler: CharacterShootingHandler by lazy {
        val handler = CharacterShootingHandler(gamePlayManagers.ecs.entityBuilder)
        handler.initialize(gamePlayManagers.dispatcher, gameSessionData, autoAim)
        handler
    }
    private val closestRayResultCallback = ClosestRayResultCallback(rayFrom, rayTo)
    override fun preUpdate(character: Entity, deltaTime: Float) {
        val aiComponent = ai.get(character)
        if (aiComponent.state == AiStatus.PLANNING) {
            val position =
                ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector3_2
                )

            val start = gameSessionData.mapData.mapGraph.getNode(position.x.toInt(), position.z.toInt())
            val transform =
                ComponentsMapper.modelInstance.get(gameSessionData.gamePlayData.player).gameModelInstance.modelInstance.transform
            val playerPosition =
                transform.getTranslation(
                    auxVector3_1
                )
            val end = gameSessionData.mapData.mapGraph.getNode(playerPosition.x.toInt(), playerPosition.z.toInt())
            aiComponent.path.clear()
            val pathFound =
                gamePlayManagers.pathFinder.searchNodePath(start, end, aiComponent.path, aiComponent.nodesToExclude)
            if (pathFound) {
                aiComponent.state = AiStatus.MOVING
                aiComponent.path.nodes.removeIndex(0)
                aiComponent.currentNode = start
            } else {
                Gdx.app.log(
                    javaClass.simpleName,
                    "Path not found from $start to $end"
                )
            }
        } else if (aiComponent.state == AiStatus.MOVING) {
            handleMovingState(character, aiComponent, deltaTime)
        } else if (aiComponent.state == AiStatus.REVERSE) {
            movementHandler.reverse()
            if (!checkIfForwardIsBlocked(
                    character,
                    closestRayResultCallback,
                    gameSessionData.physicsData.collisionWorld,
                )
            ) {
                aiComponent.state = AiStatus.PLANNING
            }
        }
    }

    private fun handleMovingState(
        character: Entity,
        aiComponent: AiComponent,
        deltaTime: Float
    ) {
        applyMovement(character, aiComponent, deltaTime)
        aimAndShoot(character, deltaTime)
    }


    private fun aimAndShoot(character: Entity, deltaTime: Float) {
        if (GameDebugSettings.AI_ATTACK_DISABLED) return

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
            gameSessionData: GameSessionData,
            callback: ClosestRayResultCallback,
            rayLength: Float
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
            gameSessionData: GameSessionData,
            callback: ClosestRayResultCallback,
            rayLength: Float
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
            gameSessionData: GameSessionData,
            callback: ClosestRayResultCallback,
            rayLength: Float
        ) {
            movementHandler.applyTurretRotation(0, character)
            if (ComponentsMapper.character.get(gameSessionData.gamePlayData.player).definition.isFlyer()) {
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
        val pathNodes = aiComponent.path.nodes
        val playerTransform =
            ComponentsMapper.modelInstance.get(gameSessionData.gamePlayData.player).gameModelInstance.modelInstance.transform
        val playerPosition =
            playerTransform.getTranslation(
                auxVector3_1
            )
        if (pathNodes.size == 0 || isPlayerFarFromDestination(playerPosition, pathNodes)) {
            aiComponent.state = AiStatus.PLANNING
            movementHandler.stopMovement()
            movementHandler.applyRotation(0, character)
            return
        }

        val transform = ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform
        val position =
            transform.getTranslation(
                auxVector3_1
            )
        val currentNode = gameSessionData.mapData.mapGraph.getNode(position.x.toInt(), position.z.toInt())
        val nextNode = pathNodes.first()
        if (currentNode == nextNode) {
            pathNodes.removeIndex(0)
            if (pathNodes.size == 0) {
                aiComponent.state = AiStatus.PLANNING
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

    private fun isPlayerFarFromDestination(
        playerPosition: Vector3,
        pathNodes: Array<MapGraphNode>
    ): Boolean {
        return playerPosition.dst2(
            auxVector3_2.set(
                pathNodes.get(pathNodes.size - 1).x.toFloat() + 0.5F,
                0F,
                pathNodes.get(pathNodes.size - 1).y.toFloat() + 0.5F
            )
        ) > 15F
    }

    object BaseRotationLessThan180 : RotationCallback {
        override fun invoke(
            movementHandler: TankMovementHandlerDesktop,
            shootingHandler: CharacterShootingHandler,
            character: Entity,
            deltaTime: Float,
            angleToTarget: Float,
            gameSessionData: GameSessionData,
            callback: ClosestRayResultCallback,
            rayLength: Float
        ) {
            if (checkIfSideIsBlocked(character, callback, gameSessionData.physicsData.collisionWorld, -90F)) {
                ai.get(character).state = AiStatus.REVERSE
                return
            }
            Gdx.app.log(
                AiTankLogic::class.java.simpleName,
                "BaseRotationLessThan180: $angleToTarget, rayFrom: $rayFrom, rayTo: $rayTo"
            )

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
            gameSessionData: GameSessionData,
            callback: ClosestRayResultCallback,
            rayLength: Float
        ) {
            if (checkIfSideIsBlocked(character, callback, gameSessionData.physicsData.collisionWorld, 90F)) {
                ai.get(character).state = AiStatus.REVERSE
                return
            }

            val facingDirection = getFacingDirection(character)
            movementHandler.applyRotation(if (facingDirection > angleToTarget) 1 else -1, character)
            movementHandler.stopMovement()
        }

    }

    object BaseRotationAnglesMatch : RotationCallback {
        const val MAX_LOOKING_AHEAD = 1F
        private val result = mutableListOf<MapGraphNode>()
        private val tileCollector: (Int, Int, MapGraph) -> Unit = { x, z, mapGraph ->
            if (x >= 0 && z >= 0 && x < mapGraph.width && z < mapGraph.depth) {
                result.add(mapGraph.getNode(x, z))
            }
        }

        override fun invoke(
            movementHandler: TankMovementHandlerDesktop,
            shootingHandler: CharacterShootingHandler,
            character: Entity,
            deltaTime: Float,
            angleToTarget: Float,
            gameSessionData: GameSessionData,
            callback: ClosestRayResultCallback,
            rayLength: Float
        ) {
            if (checkIfForwardIsBlocked(
                    character,
                    callback,
                    gameSessionData.physicsData.collisionWorld,
                )
            ) {
                val colliderModelInstanceComponent =
                    ComponentsMapper.modelInstance.get(callback.collisionObject.userData as Entity)
                val aiComponent = ai.get(character)
                if (colliderModelInstanceComponent.gameModelInstance.modelInstance.transform.getTranslation(auxVector3_1)
                        .dst2(
                            ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform.getTranslation(
                                auxVector3_2
                            )
                        )
                    < rayLength / 2F
                ) {
                    result.clear()
                    aiComponent.setNodesToExclude(result)
                    aiComponent.state = AiStatus.REVERSE
                } else {
                    wayBlockedByObstacle(callback.collisionObject.userData as Entity, gameSessionData, aiComponent)
                }
                return
            }

            movementHandler.applyRotation(0, character)
            movementHandler.thrust(character, deltaTime)
        }

        private fun wayBlockedByObstacle(
            obstacle: Entity,
            gameSessionData: GameSessionData,
            aiComponent: AiComponent
        ) {
            result.clear()
            GeneralUtils.getTilesCoveredByBoundingBox(
                obstacle,
                gameSessionData.mapData.mapGraph,
                tileCollector
            )
            aiComponent.state = AiStatus.PLANNING
            aiComponent.setNodesToExclude(result)
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
                gameSessionData,
                closestRayResultCallback,
                MAX_LOOKING_AHEAD
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
                gameSessionData,
                closestRayResultCallback,
                MAX_LOOKING_AHEAD
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
            gameSessionData: GameSessionData,
            callback: ClosestRayResultCallback,
            rayLength: Float
        )

    }

    companion object {
        private val auxVector3_1 = Vector3()
        private val auxVector3_2 = Vector3()
        private val auxVector3_3 = Vector3()
        private val auxVector2 = Vector2()
        private val auxQuaternion = Quaternion()
        private const val LOOKING_OFFSET = 0.5F
        private fun getFacingDirection(entity: Entity): Float {
            val transform = ComponentsMapper.modelInstance.get(entity).gameModelInstance.modelInstance.transform
            val rotationAroundY =
                transform.getRotation(auxQuaternion).yaw + if (transform.getRotation(auxQuaternion).yaw < 0) 360F else 0F
            return rotationAroundY
        }

        private fun checkIfForwardIsBlocked(
            character: Entity,
            callback: ClosestRayResultCallback,
            collisionWorld: btDiscreteDynamicsWorld,
        ): Boolean {
            callback.collisionObject = null
            callback.closestHitFraction = 1f
            val transform = ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform
            val position =
                transform.getTranslation(
                    auxVector3_2
                )
            val direction = auxVector3_1.set(Vector3.X).rot(transform).nor()
            callback.collisionFilterGroup = COLLISION_GROUP_GENERAL
            callback.collisionFilterMask = COLLISION_GROUP_PLAYER or COLLISION_GROUP_AI or COLLISION_GROUP_GENERAL
            val collided = rayTest(position, direction, collisionWorld, callback, Vector3.Zero, MAX_LOOKING_AHEAD) ||
                    rayTest(
                        position,
                        direction,
                        collisionWorld,
                        callback,
                        auxVector3_3.set(0F, 0F, LOOKING_OFFSET),
                        MAX_LOOKING_AHEAD
                    ) || rayTest(
                position,
                direction,
                collisionWorld,
                callback,
                auxVector3_3.set(0F, 0F, -LOOKING_OFFSET),
                MAX_LOOKING_AHEAD
            )

            if (collided) {
                Gdx.app.log(
                    AiTankLogic::class.java.simpleName,
                    "checkIfForwardIsBlocked: $collided, rayFrom: $rayFrom, rayTo: $rayTo"
                )
            }
            return collided
        }

        private fun checkIfSideIsBlocked(
            character: Entity,
            callback: ClosestRayResultCallback,
            collisionWorld: btDiscreteDynamicsWorld,
            rotationAroundY: Float
        ): Boolean {
            callback.collisionObject = null
            callback.closestHitFraction = 1f
            val transform = ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform
            val position =
                transform.getTranslation(
                    auxVector3_2
                )
            val direction = auxVector3_1.set(Vector3.X).rot(transform).rotate(Vector3.Y, rotationAroundY).nor()
            callback.collisionFilterGroup = COLLISION_GROUP_GENERAL
            callback.collisionFilterMask = COLLISION_GROUP_PLAYER or COLLISION_GROUP_GENERAL
            val collided = rayTest(position, direction, collisionWorld, callback, Vector3.Zero, 2F)

            if (collided) {
                val collider = callback.collisionObject.userData as Entity
                val ambComponent = ComponentsMapper.amb.get(collider)
                if (ambComponent != null && !ambComponent.def.isMarksNodeAsBlocked()) {
                    return false
                }
            }


            return collided
        }

        private fun rayTest(
            position: Vector3,
            direction: Vector3,
            collisionWorld: btDiscreteDynamicsWorld,
            callback: ClosestRayResultCallback,
            offset: Vector3,
            rayLength: Float
        ): Boolean {
            rayFrom.set(
                position.x,
                position.y + 0.05F,
                position.z
            ).add(offset)
            rayTo.set(rayFrom).mulAdd(direction, rayLength)
            collisionWorld.rayTest(rayFrom, rayTo, callback)
            val hasHit = callback.hasHit()
            return hasHit
        }

        private val rayFrom = Vector3()
        private val rayTo = Vector3()
    }

    override fun dispose() {
        closestRayResultCallback.dispose()
    }
}
