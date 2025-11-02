package com.gadarts.returnfire.ecs.systems.character

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.math.Matrix4
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.components.ElevatorComponent
import com.gadarts.returnfire.ecs.components.ElevatorComponent.Companion.MAX_Y
import com.gadarts.returnfire.ecs.components.physics.PhysicsComponent
import com.gadarts.returnfire.ecs.systems.data.session.GameSessionData
import com.gadarts.returnfire.ecs.systems.events.SystemEvents
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.utils.CharacterPhysicsInitializer
import com.gadarts.returnfire.utils.ModelUtils
import kotlin.math.abs
import kotlin.math.sign

class BoardingHandler(
    private val gameSessionData: GameSessionData,
    private val gamePlayManagers: GamePlayManagers
) {
    private val characterPhysicsInitializer = CharacterPhysicsInitializer(gamePlayManagers.assetsManager)
    private val elevators: ImmutableArray<Entity> by lazy {
        gamePlayManagers.ecs.engine.getEntitiesFor(
            Family.all(ElevatorComponent::class.java).get()
        )
    }

    fun updateBoarding(character: Entity, deltaTime: Float) {
        val modelInstanceComponent = ComponentsMapper.modelInstance.get(character)
        val boardingComponent = ComponentsMapper.boarding.get(
            character
        )
        val hangar = gameSessionData.mapData.elevators[boardingComponent.color]
        val elevatorTransform =
            ComponentsMapper.modelInstance.get(hangar).gameModelInstance.modelInstance.transform
        if (boardingComponent.isDeploying()) {
            if (elevatorTransform.getTranslation(auxVector1).y < MAX_Y) {
                takeStepForElevatorWithCharacter(elevatorTransform, character, MAX_Y, deltaTime)
            } else {
                val animationDone = updateBoardingAnimation(deltaTime, character)
                if (animationDone && boardingComponent.isDeploying()) {
                    deployingDone(character)
                }
            }
        } else {
            val boardingAnimation = boardingComponent.boardingAnimation
            val isAlreadyDone = boardingAnimation?.isDone() != false
            val animationDone = updateBoardingAnimation(deltaTime, character)
            if (ComponentsMapper.physics.has(character)) {
                val physicsComponent = ComponentsMapper.physics.get(character)
                val matrix4 = Matrix4(physicsComponent.rigidBody.worldTransform)
                modelInstanceComponent.gameModelInstance.modelInstance.transform =
                    matrix4
                if (modelInstanceComponent.gameModelInstance.shadow != null) {
                    modelInstanceComponent.gameModelInstance.shadow!!.transform = matrix4
                }
                gamePlayManagers.dispatcher.dispatchMessage(
                    SystemEvents.PHYSICS_COMPONENT_REMOVED_MANUALLY.ordinal,
                    character
                )
                character.remove(PhysicsComponent::class.java)
            }
            if (animationDone) {
                val elevatorPosition = elevatorTransform.getTranslation(auxVector1)
                if (elevatorPosition.y <= ElevatorComponent.BOTTOM_EDGE_Y) {
                    gamePlayManagers.dispatcher.dispatchMessage(
                        SystemEvents.CHARACTER_ONBOARDING_FINISHED.ordinal,
                        character
                    )
                    elevatorTransform.setTranslation(
                        elevatorPosition.x,
                        ElevatorComponent.BOTTOM_EDGE_Y,
                        elevatorPosition.z
                    )
                    gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.REMOVE_ENTITY.ordinal, character)
                } else {
                    takeStepForElevatorWithCharacter(
                        elevatorTransform,
                        character,
                        ElevatorComponent.BOTTOM_EDGE_Y,
                        deltaTime
                    )
                }
                if (!isAlreadyDone) {
                    gamePlayManagers.dispatcher.dispatchMessage(
                        SystemEvents.CHARACTER_ONBOARDING_ANIMATION_DONE.ordinal,
                        character
                    )
                }
            }
        }
    }

    private fun updateBoardingAnimation(
        deltaTime: Float,
        character: Entity
    ): Boolean {
        val boardingComponent = ComponentsMapper.boarding.get(character)
        val boardingAnimation = boardingComponent.boardingAnimation ?: return true

        if (boardingAnimation.isDone()) return true

        return boardingAnimation.update(
            deltaTime,
            character,
            gamePlayManagers.soundManager,
            gamePlayManagers.assetsManager
        )
    }

    private fun takeStepForElevator(
        elevatorTransform: Matrix4,
        targetY: Float,
        deltaTime: Float
    ): Float {
        val currentPosition = elevatorTransform.getTranslation(auxVector2)
        val y = currentPosition.y
        if ((y <= targetY && targetY == ElevatorComponent.BOTTOM_EDGE_Y) || (y >= MAX_Y && targetY == MAX_Y)) return 0f

        val distance = targetY - y
        val direction = distance.sign
        val absDistance = abs(distance)

        val isOnboarding = distance < 0f

        val maxSpeed = if (isOnboarding) 0.6f else 1.5f
        val minSpeed = if (isOnboarding) 0.2f else 0.3f  // Define your desired minimal constant speed
        val slowDownDistance = if (isOnboarding) 1.0f else 0.5f  // Distance at which slowing starts

        // Calculate speed with smooth deceleration
        val targetSpeed = if (absDistance < slowDownDistance) {
            val t = absDistance / slowDownDistance
            val easedT = t * t * (3f - 2f * t)
            minSpeed + (maxSpeed - minSpeed) * easedT
        } else {
            maxSpeed
        }

        // Calculate actual movement this frame, ensuring we don't overshoot
        val movementThisFrame = (targetSpeed * deltaTime).coerceAtMost(absDistance)

        // Update elevator position
        val deltaMovement = movementThisFrame * direction
        currentPosition.y += deltaMovement
        elevatorTransform.setTranslation(currentPosition)
        return deltaMovement
    }

    private fun takeStepForElevatorWithCharacter(
        elevatorTransform: Matrix4,
        character: Entity,
        targetY: Float,
        deltaTime: Float
    ) {
        val deltaMovement = takeStepForElevator(elevatorTransform, targetY, deltaTime)

        // Move character along with elevator
        ComponentsMapper.modelInstance.get(character)
            .gameModelInstance.modelInstance.transform.trn(0f, deltaMovement, 0f)
    }

    private fun deployingDone(character: Entity) {
        ComponentsMapper.boarding.get(character).boardingDone()
        characterPhysicsInitializer.initialize(
            gamePlayManagers.ecs.entityBuilder,
            character,
        )
        val turretBaseComponent = ComponentsMapper.turretBase.get(character)
        if (turretBaseComponent != null) {
            val automationComponent = ComponentsMapper.turretAutomation.get(turretBaseComponent.turret)
            if (automationComponent != null && !automationComponent.enabled) {
                automationComponent.enabled = true
            }
        }
        gamePlayManagers.dispatcher.dispatchMessage(
            SystemEvents.CHARACTER_DEPLOYMENT_DONE.ordinal,
            character
        )
    }

    fun updateElevators(deltaTime: Float) {
        for (elevator in elevators) {
            val elevatorComponent = ComponentsMapper.elevator.get(elevator)
            if (elevatorComponent.emptyOnboard) {
                takeStepForElevator(
                    ComponentsMapper.modelInstance.get(elevator).gameModelInstance.modelInstance.transform,
                    ElevatorComponent.BOTTOM_EDGE_Y,
                    deltaTime
                )
                if (ModelUtils.getPositionOfModel(elevator).y <= ElevatorComponent.BOTTOM_EDGE_Y) {
                    elevatorComponent.emptyOnboard = false
                    gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.ELEVATOR_EMPTY_ONBOARD.ordinal, elevator)
                }
            }
        }
    }

    companion object {
        private val auxVector1 = com.badlogic.gdx.math.Vector3()
        private val auxVector2 = com.badlogic.gdx.math.Vector3()

    }
}
