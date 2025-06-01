package com.gadarts.returnfire.systems.player.handlers.movement

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.gadarts.returnfire.components.CharacterComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.shared.model.definitions.TurretCharacterDefinition


abstract class VehicleMovementHandler(
    private val lateralVelocityScale: Float,
    private val rotationScale: Float,
    private val forwardForceSize: Float,
    private val reverseForceSize: Float,
    private val maxVelocity: Float,
) {

    protected open fun pushForward(rigidBody: btRigidBody, forwardDirection: Int, character: Entity, deltaTime: Float) {
        val direction = auxVector3_2.set(forwardDirection * 1F, 0F, 0F)
        val scale = if (forwardDirection > 0) forwardForceSize else reverseForceSize
        push(character, direction, scale, deltaTime)
    }

    protected fun push(
        character: Entity,
        direction: Vector3,
        scale: Float,
        deltaTime: Float,
    ) {
        val physicsComponent = ComponentsMapper.physics.get(character)
        val rigidBody = physicsComponent.rigidBody
        val newVelocity = auxVector3_1.set(rigidBody.linearVelocity)
        if (newVelocity.len2() < maxVelocity) {
            val pushDirectionRelativeToFacing =
                rigidBody.worldTransform.getRotation(auxQuaternion1)
                    .transform(direction)
            rigidBody.applyCentralForce(
                auxVector3_1.set(pushDirectionRelativeToFacing.x, 0F, pushDirectionRelativeToFacing.z)
                    .scl(scale)
            )
        }
        consumeFuelByPace(ComponentsMapper.character.get(character), deltaTime)
    }

    open fun update(
        character: Entity,
        deltaTime: Float,
    ) {
        applyLateralDamping(character)
        val characterComponent = ComponentsMapper.character.get(character)
        val definition = characterComponent.definition
        consumeFuelInIdle(ComponentsMapper.character.get(character), deltaTime)
        if (definition == TurretCharacterDefinition.TANK) {
            val rigidBody = ComponentsMapper.physics.get(character).rigidBody
            if (!rigidBody.linearVelocity.epsilonEquals(Vector3.Zero)) {
                val transform: Matrix4 = rigidBody.worldTransform
                transform.getRotation(auxQuaternion1)
                val localZ = auxVector3_1.set(Vector3.Z)
                auxQuaternion1.transform(localZ)
                val velocity: Vector3 = rigidBody.linearVelocity
                val sidewaysVelocity = velocity.dot(localZ)
                velocity.mulAdd(localZ, -sidewaysVelocity)
                rigidBody.linearVelocity = velocity
            }
        }
    }

    private fun consumeFuelInIdle(
        characterComponent: CharacterComponent,
        deltaTime: Float
    ) {
        if (!characterComponent.definition.isConsumingFuelOnIdle() || characterComponent.fuel <= 0) return

        characterComponent.idleFuelConsumptionTimer += deltaTime

        if (characterComponent.idleFuelConsumptionTimer >= IDLE_FUEL_CONSUMPTION_DURATION_IN_SECONDS) {
            characterComponent.fuel -= 1
            characterComponent.idleFuelConsumptionTimer -= IDLE_FUEL_CONSUMPTION_DURATION_IN_SECONDS
        }
    }

    private fun consumeFuelByPace(
        characterComponent: CharacterComponent,
        deltaTime: Float
    ) {
        val definition = characterComponent.definition
        if (definition.getFuelConsumptionPace() > 0 && characterComponent.fuel > 0) {
            characterComponent.fuel -= (deltaTime * definition.getFuelConsumptionPace())
        }
    }

    abstract fun thrust(
        character: Entity,
        directionX: Float = 0F,
        directionY: Float = 0F,
    )

    abstract fun reverse()

    abstract fun onMovementTouchUp(character: Entity, keycode: Int = -1)

    protected fun rotate(rigidBody: btRigidBody, clockwise: Int) {
        val newVelocity = auxVector3_1.set(rigidBody.angularVelocity)
        if (newVelocity.len2() < MAX_ROTATION) {
            val scale = clockwise * rotationScale
            rigidBody.applyTorque(auxVector3_1.set(0F, 1F, 0F).scl(scale))
            auxVector3_1.set(rigidBody.angularVelocity)
            auxVector3_1.x = 0F
            auxVector3_1.z = 0F
            rigidBody.angularVelocity = auxVector3_1
        }
    }

    abstract fun applyRotation(clockwise: Int, character: Entity)

    private fun applyLateralDamping(character: Entity) {
        if (!ComponentsMapper.physics.has(character)) return

        val rigidBody = ComponentsMapper.physics.get(character).rigidBody
        val velocity: Vector3 = rigidBody.linearVelocity
        val lateralVelocity = auxVector3_1.set(velocity.x, 0f, velocity.z)
        val dampingForce: Vector3 = lateralVelocity.scl(lateralVelocityScale)
        rigidBody.applyCentralForce(dampingForce)
    }

    abstract fun onTurretTouchPadTouchDown(deltaX: Float, deltaY: Float, character: Entity)
    abstract fun onTurretTouchPadTouchUp(character: Entity)
    open fun onReverseScreenButtonPressed(character: Entity) {}
    open fun onReverseScreenButtonReleased(character: Entity) {}
    open fun stopMovement() {

    }

    abstract fun strafe(left: Boolean)
    abstract fun isStrafing(): Boolean
    abstract fun stopStrafe()
    abstract fun pressedAlt(character: Entity)
    abstract fun pressedLeft(character: Entity)
    abstract fun pressedRight(character: Entity)
    abstract fun releasedAlt(character: Entity)
    abstract fun isThrusting(character: Entity): Boolean
    abstract fun isReversing(character: Entity): Boolean

    companion object {
        private val auxVector3_1 = Vector3()
        private val auxVector3_2 = Vector3()
        private const val MAX_ROTATION = 3F
        private val auxQuaternion1 = Quaternion()
        private const val IDLE_FUEL_CONSUMPTION_DURATION_IN_SECONDS = 5F
    }
}
