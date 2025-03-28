package com.gadarts.returnfire.systems.ai.logic

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.AiComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.model.MapGraph
import com.gadarts.returnfire.model.MapGraphPath
import com.gadarts.returnfire.systems.ai.AiStatus
import com.gadarts.returnfire.systems.ai.MapPathFinder
import com.gadarts.returnfire.systems.data.GameSessionDataGameplay
import com.gadarts.returnfire.systems.player.handlers.movement.tank.TankMovementHandlerDesktop
import kotlin.math.abs


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
                    auxVector3
                )
            val start = mapGraph.getNode(position.x.toInt(), position.z.toInt())
            val playerPosition =
                ComponentsMapper.modelInstance.get(gamePlayData.player).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector3
                )
            val end = mapGraph.getNode(playerPosition.x.toInt(), playerPosition.z.toInt())
            path.clear()
            val pathFound = pathFinder.searchNodePath(start, end, aiComponent.path)
            if (pathFound) {
                aiComponent.state = AiStatus.MOVING
                aiComponent.path.nodes.removeIndex(0)
            }
        } else if (aiComponent.state == AiStatus.MOVING) {
            applyMovement(character, aiComponent, deltaTime)
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
        val transform = ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform
        val position =
            transform.getTranslation(
                auxVector3
            )
        val currentNode = mapGraph.getNode(position.x.toInt(), position.z.toInt())
        val nextNode = aiComponent.path.nodes.first()
        if (currentNode == nextNode) {
            aiComponent.path.nodes.removeIndex(0)
            if (aiComponent.path.nodes.size == 0) {
                aiComponent.state = AiStatus.IDLE
            }
        } else {
            val rotationAroundY = transform.getRotation(auxQuaternion).yaw
            val directionToNextNode =
                auxVector2.set(nextNode.x.toFloat(), nextNode.y.toFloat()).sub(position.x, position.z).nor()
            val angleToNextNode = directionToNextNode.angleDeg()
            if (MathUtils.isEqual(rotationAroundY, angleToNextNode, 1f)) {
                movementHandler.thrust(character, deltaTime)
            } else {
                val angleDiff = abs(rotationAroundY - angleToNextNode)
                if (angleDiff > 180) {
                    movementHandler.applyRotation(if (rotationAroundY > angleToNextNode) 1 else -1, character)
                } else {
                    movementHandler.applyRotation(if (rotationAroundY > angleToNextNode) -1 else 1, character)
                }
            }
        }
    }

    companion object {
        private val auxVector3 = Vector3()
        private val auxVector2 = Vector2()
        private val path = MapGraphPath()
        private val auxQuaternion = Quaternion()
    }
}
