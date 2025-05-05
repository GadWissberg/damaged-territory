package com.gadarts.returnfire.systems.ai.logic

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback
import com.badlogic.gdx.physics.bullet.collision.btPairCachingGhostObject
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.ComponentsMapper.ai
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.model.MapGraph
import com.gadarts.returnfire.model.MapGraphCost
import com.gadarts.returnfire.model.MapGraphType
import com.gadarts.returnfire.model.graph.MapGraphNode
import com.gadarts.returnfire.systems.ai.AiStatus
import com.gadarts.returnfire.systems.ai.AiTurretStatus
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
) : AiCharacterLogic(gamePlayManagers.dispatcher), Disposable {
    private val movementHandler: TankMovementHandlerDesktop by lazy {
        val movementHandler = TankMovementHandlerDesktop()
        movementHandler
    }
    private val shootingHandler: CharacterShootingHandler by lazy {
        val handler = CharacterShootingHandler(
            gamePlayManagers.ecs.entityBuilder,
            gamePlayManagers.soundManager,
            gamePlayManagers.assetsManager.getAssetByDefinition(
                SoundDefinition.EMPTY
            )
        )
        handler.initialize(gamePlayManagers.dispatcher, gameSessionData, autoAim)
        handler
    }
    private val closestRayResultCallback = ClosestRayResultCallback(rayFrom, rayTo)

    override fun preUpdate(character: Entity, deltaTime: Float) {
        val aiComponent = ai.get(character)
        if (aiComponent.target == null) return

        val tankAiComponent = ComponentsMapper.tankAiComponent.get(character)
        val roamingEndTime = tankAiComponent.roamingEndTime
        val mapGraph = gameSessionData.mapData.mapGraph
        val path = tankAiComponent.path
        if (aiComponent.state == AiStatus.PLANNING) {
            val position =
                ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector3_2
                )

            val start = mapGraph.getNode(position.x.toInt(), position.z.toInt())
            val transform =
                ComponentsMapper.modelInstance.get(aiComponent.target).gameModelInstance.modelInstance.transform
            val targetPosition =
                transform.getTranslation(
                    auxVector3_1
                )
            val end = mapGraph.getNode(targetPosition.x.toInt(), targetPosition.z.toInt())
            path.clear()
            val pathFound =
                gamePlayManagers.pathFinder.searchNodePath(
                    start,
                    end,
                    path,
                    tankAiComponent.nodesToExclude,
                    MapGraphCost.FREE_WAY
                )
            if (pathFound) {
                aiComponent.state = AiStatus.MOVING
                path.nodes.removeIndex(0)
                tankAiComponent.currentNode = start
            } else {
                val pathFoundIncludingBlockedWAY = gamePlayManagers.pathFinder.searchNodePath(
                    start,
                    end,
                    path,
                    tankAiComponent.nodesToExclude,
                    MapGraphCost.BLOCKED_WAY
                )
                if (pathFoundIncludingBlockedWAY) {
                    aiComponent.state = AiStatus.MOVING
                    path.nodes.removeIndex(0)
                    tankAiComponent.currentNode = start
                } else {
                    activateRoaming(character)
                }
            }
        } else if (aiComponent.state == AiStatus.MOVING) {
            handleMovingState(character, deltaTime)
        } else {
            deactivateRoamingIfNeeded(character)
            if (aiComponent.state == AiStatus.REVERSE) {
                movementHandler.reverse()
                if (!checkIfForwardIsBlocked(
                        character,
                        closestRayResultCallback,
                        gameSessionData.physicsData.collisionWorld,
                        mapGraph
                    )
                ) {
                    aiComponent.state =
                        if (roamingEndTime == null) AiStatus.PLANNING else AiStatus.ROAMING
                }
            } else if (aiComponent.state == AiStatus.ROAMING) {
                val position =
                    ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform.getTranslation(
                        auxVector3_1
                    )
                val currentNode = mapGraph.getNode(position.x.toInt(), position.z.toInt())
                mapGraph.maxCost = MapGraphCost.FREE_WAY
                val connections = mapGraph.getConnections(currentNode)
                if (!connections.isEmpty) {
                    path.clear()
                    path.add(connections.random().toNode)
                    aiComponent.state = AiStatus.MOVING
                }
            }
        }
    }

    private fun deactivateRoamingIfNeeded(character: Entity) {
        val tankAiComponent = ComponentsMapper.tankAiComponent.get(character)
        val baseAiComponent = ai.get(character)
        val roamingEndTime = tankAiComponent.roamingEndTime
        if (roamingEndTime == null || roamingEndTime < TimeUtils.millis()) {
            tankAiComponent.roamingEndTime = null
            if (baseAiComponent.state == AiStatus.ROAMING) {
                baseAiComponent.state = AiStatus.PLANNING
            }
        }
    }

    private fun activateRoaming(tank: Entity) {
        val tankAiComponent = ComponentsMapper.tankAiComponent.get(tank)
        val baseAiComponent = ai.get(tank)
        baseAiComponent.state = AiStatus.ROAMING
        tankAiComponent.roamingEndTime = System.currentTimeMillis() + 10000L
    }

    private fun handleMovingState(
        tank: Entity,
        deltaTime: Float
    ) {
        applyMovement(tank, deltaTime)
        handleTurret(tank, deltaTime)
        deactivateRoamingIfNeeded(tank)
    }


    private fun shouldSkipHandlingTurret(baseTurret: Entity): Boolean {
        if (GameDebugSettings.AI_ATTACK_DISABLED) return true

        val playerTransform =
            ComponentsMapper.modelInstance.get(gameSessionData.gamePlayData.player).gameModelInstance.modelInstance.transform
        val destinationPosition = playerTransform.getTranslation(auxVector3_1)
        val characterPosition =
            ComponentsMapper.modelInstance.get(baseTurret).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector3_2
            )
        val turret = ComponentsMapper.turretBase.get(baseTurret).turret
        if (destinationPosition.set(destinationPosition.x, 0F, destinationPosition.z)
                .dst2(characterPosition.set(characterPosition.x, 0F, characterPosition.z)) > 88F
        ) {
            stopAttack()
            movementHandler.applyTurretRotation(0, turret)
            return true
        }

        return false
    }

    private fun stopAttack() {
        shootingHandler.stopSecondaryShooting()
        shootingHandler.stopPrimaryShooting()
    }

    private fun handleTurret(character: Entity, deltaTime: Float) {
        if (shouldSkipHandlingTurret(character)) return

        val playerTransform =
            ComponentsMapper.modelInstance.get(gameSessionData.gamePlayData.player).gameModelInstance.modelInstance.transform
        val destinationPosition = playerTransform.getTranslation(auxVector3_1)
        val turret = ComponentsMapper.turretBase.get(character).turret
        val aiTurretComponent = ComponentsMapper.aiTurret.get(turret)
        if (aiTurretComponent.state == AiTurretStatus.ATTACK) {
            rotateAndEngageTurret(
                auxVector2.set(destinationPosition.x, destinationPosition.z),
                turret,
                deltaTime,
                TurretRotationAnglesMatchForAttack
            )
            if (aiTurretComponent.nextLookingAroundTime < System.currentTimeMillis()) {
                aiTurretComponent.state = AiTurretStatus.LOOK_AROUND
                aiTurretComponent.setDestination(
                    auxVector2.set(
                        MathUtils.random(gameSessionData.mapData.mapGraph.width).toFloat(),
                        MathUtils.random(gameSessionData.mapData.mapGraph.depth).toFloat()
                    )
                )
                stopAttack()
            }
        } else if (aiTurretComponent.state == AiTurretStatus.LOOK_AROUND) {
            rotateAndEngageTurret(
                ComponentsMapper.aiTurret.get(turret).getDestination(auxVector2),
                turret,
                deltaTime,
                TurretRotationAnglesMatchForLookingAround
            )
        }
    }

    private fun rotateAndEngageTurret(
        destinationPosition: Vector2,
        turret: Entity,
        deltaTime: Float,
        callbackForAnglesMatch: RotationCallback
    ) {
        rotateAndEngage(
            destinationPosition,
            turret,
            deltaTime,
            callbackForAnglesMatch,
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

    object TurretRotationAnglesMatchForAttack : RotationCallback {
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
            val target = ComponentsMapper.aiTurret.get(character).target ?: return

            if (ComponentsMapper.character.get(target).definition.isFlyer()) {
                shootingHandler.startSecondaryShooting(gameSessionData.gamePlayData.player)
            } else {
                shootingHandler.startPrimaryShooting(null)
            }
        }

    }

    object TurretRotationAnglesMatchForLookingAround : RotationCallback {
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
            val aiTurretComponent = ComponentsMapper.aiTurret.get(character)
            aiTurretComponent.state = AiTurretStatus.ATTACK
            aiTurretComponent.target = gameSessionData.gamePlayData.player
            aiTurretComponent.nextLookingAroundTime = System.currentTimeMillis() + MathUtils.random(12000L)
        }

    }

    override fun update(character: Entity, deltaTime: Float) {
        movementHandler.update(character, deltaTime)
        shootingHandler.update(character)
        val aiComponent = ai.get(character)
        if (shouldReturnToBase(character)) {
            aiComponent.state = AiStatus.PLANNING
            ComponentsMapper.tankAiComponent.get(character).roamingEndTime = 0
            aiComponent.target = gameSessionData.mapData.stages[ComponentsMapper.boarding.get(character).color]
        }
    }


    private fun applyMovement(
        tank: Entity,
        deltaTime: Float
    ) {
        val baseAiComponent = ai.get(tank)
        val tankAiComponent = ComponentsMapper.tankAiComponent.get(tank)
        val pathNodes = tankAiComponent.path.nodes
        val target = baseAiComponent.target
        val targetTransform =
            ComponentsMapper.modelInstance.get(target).gameModelInstance.modelInstance.transform
        val targetPosition =
            targetTransform.getTranslation(
                auxVector3_1
            )
        val returningToBase = ComponentsMapper.hangarStage.has(target)
        val emptyPath = pathNodes.size == 0
        if (tankAiComponent.roamingEndTime == null && target != null && ComponentsMapper.character.has(target) && auxVector3_2.set(
                targetPosition.x,
                0F,
                targetPosition.z
            ).dst2(getPositionOfCharacter(tank)) < 4F
        ) {
            activateRoaming(tank)
            return
        } else {
            if (emptyPath
                || (!returningToBase && tankAiComponent.roamingEndTime == null && isTargetFarFromDestination(
                    targetPosition,
                    pathNodes
                ))
            ) {
                if (returningToBase) {
                    pathNodes.add(
                        gameSessionData.mapData.mapGraph.getNode(
                            targetPosition.x.toInt(),
                            targetPosition.z.toInt()
                        )
                    )
                    onboard(
                        tank,
                        0.1F,
                    )
                } else {
                    baseAiComponent.state =
                        if (tankAiComponent.roamingEndTime == null) AiStatus.PLANNING else AiStatus.ROAMING
                    movementHandler.stopMovement()
                    movementHandler.applyRotation(0, tank)
                }
                return
            }
        }
        val position =
            getPositionOfCharacter(tank)
        val currentNode = gameSessionData.mapData.mapGraph.getNode(position.x.toInt(), position.z.toInt())
        val nextNode = pathNodes.first()
        if (currentNode == nextNode) {
            pathNodes.removeIndex(0)
            if (pathNodes.isEmpty) {
                baseAiComponent.state =
                    if (tankAiComponent.roamingEndTime == null) AiStatus.PLANNING else AiStatus.ROAMING
            }
        } else {
            val centerOrCornerOfNode = if (pathNodes.size != 1 || !returningToBase) 0.5F else 0F
            rotateAndEngage(
                auxVector2.set(nextNode.x.toFloat(), nextNode.y.toFloat())
                    .add(centerOrCornerOfNode, centerOrCornerOfNode),
                tank,
                deltaTime,
                BaseRotationAnglesMatch,
                BaseRotationGreaterThan180,
                BaseRotationLessThan180,
                32f
            )
        }
    }

    private fun isTargetFarFromDestination(
        playerPosition: Vector3,
        pathNodes: Array<MapGraphNode>
    ): Boolean {
        return playerPosition.set(playerPosition.x, 0F, playerPosition.z).dst2(
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
        const val MAX_LOOKING_AHEAD = 0.2F
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
                    gameSessionData.mapData.mapGraph,
                )
            ) {
                if (callback.collisionObject != null) {
                    val collisionObjectEntity = callback.collisionObject.userData as Entity
                    val colliderModelInstanceComponent =
                        ComponentsMapper.modelInstance.get(collisionObjectEntity)
                    val aiComponent = ai.get(character)
                    val tankAiComponent = ComponentsMapper.tankAiComponent.get(character)
                    if (colliderModelInstanceComponent.gameModelInstance.modelInstance.transform.getTranslation(
                            auxVector3_1
                        )
                            .dst2(
                                ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform.getTranslation(
                                    auxVector3_2
                                )
                            )
                        < rayLength / 2F
                    ) {
                        result.clear()
                        tankAiComponent.setNodesToExclude(result)
                        aiComponent.state = AiStatus.REVERSE
                    } else {
                        wayBlockedByObstacle(
                            callback.collisionObject.userData as Entity,
                            gameSessionData,
                            character
                        )
                    }
                }
                movementHandler.stopMovement()
                return
            }

            movementHandler.applyRotation(0, character)
            movementHandler.thrust(character, deltaTime)
        }

        private fun wayBlockedByObstacle(
            obstacle: Entity,
            gameSessionData: GameSessionData,
            character: Entity
        ) {
            result.clear()
            GeneralUtils.getTilesCoveredByBoundingBox(
                obstacle,
                gameSessionData.mapData.mapGraph,
                tileCollector
            )
            val aiComponent = ai.get(character)
            val tankAiComponent = ComponentsMapper.tankAiComponent.get(character)
            aiComponent.state =
                if (tankAiComponent.roamingEndTime == null) AiStatus.PLANNING else AiStatus.ROAMING
            tankAiComponent.setNodesToExclude(result)
        }

    }

    private fun rotateAndEngage(
        target: Vector2,
        entityToRotate: Entity,
        deltaTime: Float,
        rotationCallback: RotationCallback,
        baseRotationGreaterThan180: RotationCallback,
        baseRotationLessThan180: RotationCallback,
        epsilon: Float
    ) {
        if (gameSessionData.gamePlayData.player == null) return

        val position = getPositionOfCharacter(entityToRotate)
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

    private fun getPositionOfCharacter(character: Entity): Vector3 {
        val transform = ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform
        val position =
            transform.getTranslation(
                auxVector3_3
            )
        return position
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
        private const val RAY_FORWARD_SIDE_OFFSET = 0.5F
        private const val RAY_FORWARD_OFFSET = 0.5F
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
            mapGraph: MapGraph,
        ): Boolean {
            callback.collisionObject = null
            callback.closestHitFraction = 1f
            val transform = ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform
            val position =
                transform.getTranslation(
                    auxVector3_2
                )
            val direction = auxVector3_1.set(Vector3.X).rot(transform).nor()
            val positionInFront = auxVector3_3.set(position).add(direction).add(direction).add(direction)
            val nodeX = positionInFront.x.toInt()
            val nodeY = positionInFront.z.toInt()
            if (nodeX < 0
                || nodeY < 0
                || nodeX >= mapGraph.width
                || nodeY >= mapGraph.depth
                || mapGraph.getNode(nodeX, nodeY).type == MapGraphType.WATER
            ) {
                return true
            }

            callback.collisionFilterGroup = COLLISION_GROUP_GENERAL
            callback.collisionFilterMask = COLLISION_GROUP_PLAYER or COLLISION_GROUP_AI or COLLISION_GROUP_GENERAL
            val collided = rayTest(
                position,
                direction,
                collisionWorld,
                callback,
                Vector3(RAY_FORWARD_OFFSET, 0F, 0F).rot(transform),
                MAX_LOOKING_AHEAD
            ) ||
                    rayTest(
                        position,
                        direction,
                        collisionWorld,
                        callback,
                        auxVector3_3.set(RAY_FORWARD_OFFSET, 0F, RAY_FORWARD_SIDE_OFFSET).rot(transform),
                        MAX_LOOKING_AHEAD
                    ) || rayTest(
                position,
                direction,
                collisionWorld,
                callback,
                auxVector3_3.set(RAY_FORWARD_OFFSET, 0F, -RAY_FORWARD_SIDE_OFFSET).rot(transform),
                MAX_LOOKING_AHEAD
            )

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
            val collided = rayTest(
                position,
                direction,
                collisionWorld,
                callback,
                auxVector3_3.set(0.3F, 0F, 0F).rot(transform),
                0.5F
            )

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
