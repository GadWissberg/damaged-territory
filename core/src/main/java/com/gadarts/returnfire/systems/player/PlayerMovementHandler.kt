package com.gadarts.returnfire.systems.player

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.*
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.math.collision.Sphere
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.SoundPlayer
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.SfxDefinitions
import com.gadarts.returnfire.components.BoxCollisionComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.ComponentsMapper.player
import com.gadarts.returnfire.model.GameMap
import com.gadarts.returnfire.systems.GameSessionData.Companion.REGION_SIZE
import com.gadarts.returnfire.systems.SystemEvents
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow


class PlayerMovementHandler() {
    private lateinit var assetsManager: GameAssetManager
    private lateinit var camera: PerspectiveCamera
    private lateinit var collisionEntities: ImmutableArray<Entity>
    private var tiltAnimationHandler = TiltAnimationHandler()
    private var rotToAdd = 0F
    private var desiredDirectionChanged: Boolean = false
    private val desiredVelocity = Vector2()
    private val prevPos = Vector3()

    fun onTouchUp() {
        desiredVelocity.setZero()
    }

    private fun handleRotation(deltaTime: Float, player: Entity) {
        if (desiredDirectionChanged && !desiredVelocity.isZero) {
            calculateRotation(deltaTime, player)
        } else {
            tiltAnimationHandler.lowerRotationTilt()
        }
        applyRotation(player)
    }

    private fun handleAcceleration(player: Entity) {
        val currentVelocity =
            ComponentsMapper.player.get(player).getCurrentVelocity(auxVector2_1)
        if (desiredVelocity.len2() > 0.5F) {
            currentVelocity.setLength2(
                min(
                    currentVelocity.len2() + (ACCELERATION),
                    MAX_SPEED
                )
            )
            tiltAnimationHandler.onAcceleration()
        } else {
            currentVelocity.setLength2(
                max(
                    currentVelocity.len2() - (DECELERATION),
                    1F
                )
            )
            tiltAnimationHandler.onDeceleration()
        }
        ComponentsMapper.player.get(player).setCurrentVelocity(currentVelocity)
    }

    private fun calculateRotation(deltaTime: Float, player: Entity) {
        val rotBefore = rotToAdd
        updateRotationStep(player)
        val currentVelocity =
            ComponentsMapper.player.get(player).getCurrentVelocity(auxVector2_1)
        val diff = abs(currentVelocity.angleDeg() - desiredVelocity.angleDeg())
        if ((rotBefore < 0 && rotToAdd < 0) || (rotBefore > 0 && rotToAdd > 0) && diff > ROT_EPSILON) {
            rotate(currentVelocity, deltaTime, player)
        } else {
            desiredDirectionChanged = false
        }
    }

    private fun rotate(currentVelocity: Vector2, deltaTime: Float, player: Entity) {
        currentVelocity.rotateDeg(rotToAdd * deltaTime)
        ComponentsMapper.player.get(player).setCurrentVelocity(currentVelocity)
        tiltAnimationHandler.onRotation(rotToAdd)
    }

    private fun updateRotationStep(player: Entity) {
        if (desiredVelocity.isZero) return
        val playerComponent = ComponentsMapper.player.get(player)
        val diff =
            desiredVelocity.angleDeg() - playerComponent.getCurrentVelocity(auxVector2_1)
                .angleDeg()
        val negativeRotation = auxVector2_1.set(1F, 0F).setAngleDeg(diff).angleDeg() > 180
        rotToAdd = if (negativeRotation && rotToAdd < 0) {
            max(rotToAdd - ROTATION_INCREASE, -MAX_ROTATION_STEP)
        } else if (!negativeRotation && rotToAdd > 0) {
            min(rotToAdd + ROTATION_INCREASE, MAX_ROTATION_STEP)
        } else {
            INITIAL_ROTATION_STEP * (if (negativeRotation) -1F else 1F)
        }
    }

    private fun activateStrafing(player: Entity) {
        val playerComponent = ComponentsMapper.player.get(player)
        playerComponent.strafing =
            ComponentsMapper.modelInstance.get(player).modelInstance.transform.getRotation(
                auxQuat
            ).getAngleAround(
                Vector3.Y
            )
        tiltAnimationHandler.onStrafeActivated()
    }

    fun onTouchPadTouched(deltaX: Float, deltaY: Float, player: Entity) {
        if (deltaX != 0F || deltaY != 0F) {
            updateDesiredDirection(deltaX, deltaY, player)
        } else {
            desiredVelocity.setZero()
        }
    }

    private fun updateDesiredDirection(deltaX: Float, deltaY: Float, player: Entity) {
        desiredVelocity.set(deltaX, deltaY)
        desiredDirectionChanged = true
        updateRotationStep(player)
    }

    fun update(
        player: Entity,
        deltaTime: Float,
        currentMap: GameMap,
        soundPlayer: SoundPlayer,
        dispatcher: MessageDispatcher
    ) {
        handleRotation(deltaTime, player)
        handleAcceleration(player)
        val transform = ComponentsMapper.modelInstance.get(player).modelInstance.transform
        transform.getTranslation(prevPos)
        applyMovementWithRegionCheck(player, deltaTime, currentMap, dispatcher)
        updateBlastVelocity(player)
        checkCollisions(player, soundPlayer)
        tiltAnimationHandler.update(player)
    }

    private fun updateBlastVelocity(player: Entity) {
        val playerComponent = ComponentsMapper.player.get(player)
        val blastVelocity = playerComponent.getBlastVelocity(auxVector2_1)
        if (blastVelocity.len2() > 0F) {
            blastVelocity.setLength2(max(blastVelocity.len2() - 0.1F, 0F))
            playerComponent.setBlastVelocity(blastVelocity)
        }
    }

    private fun checkCollisions(player: Entity, soundPlayer: SoundPlayer) {
        val radius = ComponentsMapper.sphereCollision.get(player).radius
        for (entity in collisionEntities) {
            if (player != entity) {
                checkCollision(entity, player, radius, soundPlayer)
            }
        }
    }

    private fun checkCollision(
        entity: Entity,
        player: Entity,
        radius: Float,
        soundPlayer: SoundPlayer
    ) {
        ComponentsMapper.boxCollision.get(entity).getBoundingBox(auxBoundingBox_1)
        val comp = ComponentsMapper.modelInstance.get(player)
        val pos = auxSphere.center.set(comp.modelInstance.transform.getTranslation(auxVector3_1))
        auxSphere.radius = radius
        if (intersectsWith(auxBoundingBox_1, auxSphere)) {
            applyCollision(pos, player, soundPlayer)
        }
    }

    private fun applyCollision(
        pos: Vector3,
        player: Entity,
        soundPlayer: SoundPlayer
    ) {
        val playerComp = ComponentsMapper.player.get(player)
        val v = playerComp.getCurrentVelocity(auxVector2_2)
        if (v.len2() > 2F) {
            soundPlayer.playPositionalSound(
                assetsManager.getAssetByDefinition(SfxDefinitions.CRASH),
                true,
                player,
                camera
            )
        }
        val dirToPlayer = pos.sub(auxBoundingBox_1.getCenter(auxVector3_2)).nor()
        val blastVelocity = dirToPlayer.scl(max(v.len2() * 0.4F, 1F))
        playerComp.setBlastVelocity(auxVector2_1.set(blastVelocity.x, blastVelocity.z))
        playerComp.setCurrentVelocity(v.scl(0.2F))
        ComponentsMapper.modelInstance.get(player).modelInstance.transform.setTranslation(prevPos)
    }

    @Suppress("SameParameterValue")
    private fun intersectsWith(boundingBox: BoundingBox, sphere: Sphere): Boolean {
        var dmin = 0f
        val center: Vector3 = sphere.center
        val bmin = boundingBox.min
        val bmax = boundingBox.max
        if (center.x < bmin.x) {
            dmin += (center.x - bmin.x).toDouble().pow(2.0).toFloat()
        } else if (center.x > bmax.x) {
            dmin += (center.x - bmax.x).toDouble().pow(2.0).toFloat()
        }
        if (center.y < bmin.y) {
            dmin += (center.y - bmin.y).toDouble().pow(2.0).toFloat()
        } else if (center.y > bmax.y) {
            dmin += (center.y - bmax.y).toDouble().pow(2.0).toFloat()
        }
        if (center.z < bmin.z) {
            dmin += (center.z - bmin.z).toDouble().pow(2.0).toFloat()
        } else if (center.z > bmax.z) {
            dmin += (center.z - bmax.z).toDouble().pow(2.0).toFloat()
        }
        return dmin <= sphere.radius.pow(2F)
    }

    private fun applyMovementWithRegionCheck(
        p: Entity,
        delta: Float,
        currentMap: GameMap,
        dispatcher: MessageDispatcher
    ) {
        val transform = ComponentsMapper.modelInstance.get(p).modelInstance.transform
        val currentPosition = transform.getTranslation(auxVector3_2)
        val prevHorizontalIndex = floor(currentPosition.x / REGION_SIZE)
        val prevVerticalIndex = floor(currentPosition.z / REGION_SIZE)
        applyMovement(delta, p, currentMap, player.get(p).getCurrentVelocity(auxVector2_1))
        applyMovement(delta, p, currentMap, player.get(p).getBlastVelocity(auxVector2_1))
        val newPosition = transform.getTranslation(auxVector3_2)
        val newHorizontalIndex = floor(newPosition.x / REGION_SIZE)
        val newVerticalIndex = floor(newPosition.z / REGION_SIZE)
        if (prevHorizontalIndex != newHorizontalIndex || prevVerticalIndex != newVerticalIndex) {
            dispatcher.dispatchMessage(SystemEvents.PLAYER_ENTERED_NEW_REGION.ordinal)
        }
    }

    fun onTouchDown(lastTouchDown: Long, player: Entity) {
        if (TimeUtils.timeSinceMillis(lastTouchDown) <= STRAFE_PRESS_INTERVAL) {
            activateStrafing(player)
        } else {
            deactivateStrafing(player)
        }
    }

    private fun deactivateStrafing(player: Entity) {
        val playerComponent = ComponentsMapper.player.get(player)
        if (playerComponent.strafing != null) {
            val currentVelocity = playerComponent.getCurrentVelocity(auxVector2_1)
            val newVelocity = currentVelocity.setAngleDeg(playerComponent.strafing!!)
            playerComponent.setCurrentVelocity(newVelocity)
        }
        playerComponent.strafing = null
    }

    private fun applyMovement(
        deltaTime: Float,
        player: Entity,
        currentMap: GameMap,
        velocity: Vector2
    ) {
        if (velocity.len2() > 1F) {
            val step = auxVector3_1.set(velocity.x, 0F, -velocity.y)
            step.setLength2(step.len2() - 1F).scl(deltaTime)
            val transform = ComponentsMapper.modelInstance.get(player).modelInstance.transform
            transform.trn(step)
            clampPosition(transform, currentMap)
        }
    }

    private fun clampPosition(
        transform: Matrix4,
        currentMap: GameMap,
    ) {
        val newPos = transform.getTranslation(auxVector3_1)
        newPos.x = MathUtils.clamp(newPos.x, 0F, currentMap.tilesMapping.size.toFloat())
        newPos.z = MathUtils.clamp(newPos.z, 0F, currentMap.tilesMapping[0].size.toFloat())
        transform.setTranslation(newPos)
    }

    private fun applyRotation(player: Entity) {
        val transform = ComponentsMapper.modelInstance.get(player).modelInstance.transform
        val position = transform.getTranslation(auxVector3_1)
        val playerComponent = ComponentsMapper.player.get(player)
        val currentVelocity = playerComponent.getCurrentVelocity(auxVector2_1)
        transform.setToRotation(
            Vector3.Y,
            (if (playerComponent.strafing != null) playerComponent.strafing else currentVelocity.angleDeg())!!
        )
        transform.rotate(Vector3.Z, -IDLE_Z_TILT_DEGREES)
        transform.setTranslation(position)
    }

    fun initialize(engine: Engine, assetsManager: GameAssetManager, camera: PerspectiveCamera) {
        val family = Family.all(BoxCollisionComponent::class.java).get()
        collisionEntities = engine.getEntitiesFor(family)
        this.assetsManager = assetsManager
        this.camera = camera
    }

    companion object {
        private const val MAX_ROTATION_STEP = 200F
        private const val ROTATION_INCREASE = 2F
        private const val INITIAL_ROTATION_STEP = 6F
        private val auxVector2_1 = Vector2()
        private val auxVector2_2 = Vector2()
        private val auxVector3_1 = Vector3()
        private val auxVector3_2 = Vector3()
        private val auxQuat = Quaternion()
        private const val ROT_EPSILON = 0.5F
        private const val MAX_SPEED = 14F
        private const val ACCELERATION = 0.04F
        private const val DECELERATION = 0.06F
        private const val IDLE_Z_TILT_DEGREES = 12F
        private const val STRAFE_PRESS_INTERVAL = 500
        private val auxBoundingBox_1 = BoundingBox()
        private val auxSphere = Sphere(Vector3(), 0F)
    }
}
