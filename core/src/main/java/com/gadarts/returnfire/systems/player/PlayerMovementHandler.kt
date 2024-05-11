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
import com.gadarts.returnfire.SoundPlayer
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.SfxDefinitions
import com.gadarts.returnfire.components.BoxCollisionComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.ComponentsMapper.player
import com.gadarts.returnfire.model.GameMap
import com.gadarts.returnfire.systems.GameSessionData.Companion.REGION_SIZE
import com.gadarts.returnfire.systems.SystemEvents
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow


class PlayerMovementHandler {
    private var rotating: Float = 0.0f
    private lateinit var assetsManager: GameAssetManager
    private lateinit var camera: PerspectiveCamera
    private lateinit var collisionEntities: ImmutableArray<Entity>
    private var tiltAnimationHandler = TiltAnimationHandler()
    private val prevPos = Vector3()

    private fun handleAcceleration(player: Entity) {
        val playerComponent = ComponentsMapper.player.get(player)
        if (playerComponent.thrust > 0F) {
            playerComponent.currentVelocity =
                min(
                    playerComponent.currentVelocity + (ACCELERATION),
                    MAX_SPEED
                )
            tiltAnimationHandler.tilt = true
        } else {
            playerComponent.currentVelocity = max(
                playerComponent.currentVelocity - (DECELERATION),
                1F
            )
            tiltAnimationHandler.tilt = false
        }
    }


    fun thrust(player: Entity, value: Float) {
        ComponentsMapper.player.get(player).thrust = value
    }

    fun update(
        player: Entity,
        deltaTime: Float,
        currentMap: GameMap,
        soundPlayer: SoundPlayer,
        dispatcher: MessageDispatcher
    ) {
        handleRotation(player)
        handleAcceleration(player)
        val transform = ComponentsMapper.modelInstance.get(player).modelInstance.transform
        transform.getTranslation(prevPos)
        applyMovementWithRegionCheck(player, deltaTime, currentMap, dispatcher)
        updateBlastVelocity(player)
        checkCollisions(player, soundPlayer)
        tiltAnimationHandler.update(player)
    }

    private fun handleRotation(player: Entity) {
        if (rotating != 0F) {
            val transform = ComponentsMapper.modelInstance.get(player).modelInstance.transform
            val position = auxVector3_1
            transform.getTranslation(position)
            val translateToOrigin = auxMatrix1.idt().setTranslation(-position.x, -position.y, -position.z)
            val additionalRotation = auxMatrix2.idt().setToRotation(Vector3.Y, rotating)
            val translateBack = auxMatrix3.idt().trn(position)
            val finalTransform = auxMatrix4.idt()
            finalTransform.set(translateBack).mul(additionalRotation).mul(translateToOrigin).mul(transform)
            transform.set(finalTransform)
        }
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
        if (playerComp.currentVelocity > 2F) {
            soundPlayer.playPositionalSound(
                assetsManager.getAssetByDefinition(SfxDefinitions.CRASH),
                true,
                player,
                camera
            )
        }
        val dirToPlayer = pos.sub(auxBoundingBox_1.getCenter(auxVector3_2)).nor()
        val blastVelocity = dirToPlayer.scl(max(playerComp.currentVelocity * 0.4F, 1F))
        playerComp.setBlastVelocity(auxVector2_1.set(blastVelocity.x, blastVelocity.z))
        playerComp.currentVelocity *= 0.2F
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
        applyMovement(
            delta,
            p,
            currentMap,
            auxVector2_1.set(1F, 0F).setAngleDeg(transform.getRotation(auxQuat).getAngleAround(Vector3.Y))
                .scl(player.get(p).currentVelocity)
        )
        applyMovement(delta, p, currentMap, player.get(p).getBlastVelocity(auxVector2_1))
        val newPosition = transform.getTranslation(auxVector3_2)
        val newHorizontalIndex = floor(newPosition.x / REGION_SIZE)
        val newVerticalIndex = floor(newPosition.z / REGION_SIZE)
        if (prevHorizontalIndex != newHorizontalIndex || prevVerticalIndex != newVerticalIndex) {
            dispatcher.dispatchMessage(SystemEvents.PLAYER_ENTERED_NEW_REGION.ordinal)
        }
    }

    private fun applyMovement(
        deltaTime: Float,
        player: Entity,
        currentMap: GameMap,
        velocity: Vector2
    ) {
        if (velocity.len2() > 1F) {
            val transform = ComponentsMapper.modelInstance.get(player).modelInstance.transform
            val step = auxVector3_1.set(velocity.x, 0F, -velocity.y)
            step.setLength2(step.len2() - 1F).scl(deltaTime)
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

    fun initialize(engine: Engine, assetsManager: GameAssetManager, camera: PerspectiveCamera, player: Entity) {
        tiltAnimationHandler.init(player)
        val family = Family.all(BoxCollisionComponent::class.java).get()
        collisionEntities = engine.getEntitiesFor(family)
        this.assetsManager = assetsManager
        this.camera = camera
    }

    fun rotate(angle: Float) {
        this.rotating = angle
    }

    companion object {
        private val auxVector2_1 = Vector2()
        private val auxVector3_1 = Vector3()
        private val auxVector3_2 = Vector3()
        private const val MAX_SPEED = 7F
        private const val ACCELERATION = 0.02F
        private const val DECELERATION = 0.06F
        private val auxBoundingBox_1 = BoundingBox()
        private val auxSphere = Sphere(Vector3(), 0F)
        private val auxMatrix1 = Matrix4()
        private val auxMatrix2 = Matrix4()
        private val auxMatrix3 = Matrix4()
        private val auxMatrix4 = Matrix4()
        private val auxQuat = Quaternion()
    }
}
