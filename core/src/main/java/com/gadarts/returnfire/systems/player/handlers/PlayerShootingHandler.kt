package com.gadarts.returnfire.systems.player.handlers

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btPairCachingGhostObject
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.arm.ArmComponent
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.events.data.CharacterWeaponShotEventData
import com.gadarts.returnfire.systems.player.PlayerSystem
import kotlin.math.abs
import kotlin.math.min

class PlayerShootingHandler(private val entityBuilder: EntityBuilder) {
    private lateinit var autoAim: btPairCachingGhostObject
    private lateinit var gameSessionData: GameSessionData
    private lateinit var dispatcher: MessageDispatcher
    var secondaryCreationSide = false
    private var priShooting: Boolean = false
    private var secShooting: Boolean = false

    fun initialize(
        dispatcher: MessageDispatcher,
        gameSessionData: GameSessionData,
        autoAim: btPairCachingGhostObject,
    ) {
        this.gameSessionData = gameSessionData
        this.dispatcher = dispatcher
        this.autoAim = autoAim
    }

    fun update() {
        var armComp: ArmComponent = ComponentsMapper.primaryArm.get(gameSessionData.gameplayData.player)
        updateAutoAim(armComp)
        handleShooting(
            priShooting,
            armComp,
            SystemEvents.CHARACTER_WEAPON_ENGAGED_PRIMARY,
        )
        if (ComponentsMapper.secondaryArm.has(gameSessionData.gameplayData.player)) {
            armComp = ComponentsMapper.secondaryArm.get(gameSessionData.gameplayData.player)
            handleShooting(
                secShooting,
                armComp,
                SystemEvents.CHARACTER_WEAPON_ENGAGED_SECONDARY,
            )
        }
    }

    private fun updateAutoAim(armComp: ArmComponent) {
        val rigidBody = autoAim
        val player = gameSessionData.gameplayData.player
        val turretBaseComponent = ComponentsMapper.turretBase.get(player)
        val playerModelInstance =
            if (turretBaseComponent != null) {
                ComponentsMapper.modelInstance.get(turretBaseComponent.turret).gameModelInstance.modelInstance
            } else {
                ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance
            }
        val rotation = playerModelInstance.transform.getRotation(
            auxQuat2
        )
        rotation.setEulerAngles(rotation.yaw, 0F, 0F)
        val autoAimTransform = auxMatrix2.idt().set(
            rotation
        ).setTranslation(
            playerModelInstance.transform.getTranslation(
                auxVector3_1
            )
        ).rotate(
            Vector3.Z,
            90F + armComp.armProperties.renderData.initialRotationAroundZ
        )
            .translate(0F, -PlayerSystem.AUTO_AIM_HEIGHT / 2F, 0F)
        rigidBody.worldTransform = autoAimTransform
    }

    private fun handleShooting(
        shooting: Boolean,
        armComp: ArmComponent,
        event: SystemEvents,
    ) {
        if (!shooting) return

        val now = TimeUtils.millis()
        if (armComp.loaded <= now) {
            val player = gameSessionData.gameplayData.player
            val modelInstanceComponent = ComponentsMapper.modelInstance.get(player)
            val transform = modelInstanceComponent.gameModelInstance.modelInstance.transform
            armComp.displaySpark = now
            armComp.loaded = now + armComp.armProperties.reloadDuration
            val direction =
                if (!ComponentsMapper.turretBase.has(player) || ComponentsMapper.turret.get(
                        ComponentsMapper.turretBase.get(
                            player
                        ).turret
                    ).cannon == null
                ) {
                    val rotation =
                        transform.getRotation(
                            auxQuat
                        )
                    auxMatrix.set(
                        rotation.setEulerAngles(rotation.yaw, 0F, 0F)
                    )
                } else {
                    val cannon =
                        ComponentsMapper.turret.get(ComponentsMapper.turretBase.get(player).turret).cannon
                    val direction = ComponentsMapper.modelInstance.get(cannon).gameModelInstance.modelInstance.transform
                    val particleEffect = entityBuilder.begin().addParticleEffectComponent(
                        direction.getTranslation(auxVector3_1),
                        gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE_SMALL),
                        parentRelativePosition = auxVector3_2.set(0.5F, 0F, 0F),
                    ).finishAndAddToEngine()
                    ComponentsMapper.particleEffect.get(particleEffect).parent = cannon
                    direction
                }
            val target = handleAutoAim(transform)
            if (target == null) {
                CharacterWeaponShotEventData.setWithDirection(
                    player,
                    direction,
                )
            } else {
                CharacterWeaponShotEventData.setWithTarget(player, target)
            }
            dispatcher.dispatchMessage(event.ordinal)
        }
    }

    private fun handleAutoAim(transform: Matrix4): Entity? {
        val overlappingPairs = autoAim.overlappingPairs
        val size = min(overlappingPairs.size(), 6)
        var closestEnemy: Entity? = null
        if (size > 0) {
            closestEnemy = null
            val playerPosition = transform.getTranslation(auxVector3_2)
            val playerDirection = transform.getRotation(auxQuat).transform(auxVector3_3.set(Vector3.X))
            var closestDistance = Float.MAX_VALUE
            for (i in 0 until size) {
                val enemy = overlappingPairs.atConst(i).userData
                val gameModelInstance =
                    ComponentsMapper.modelInstance.get(enemy as Entity).gameModelInstance
                val enemyPosition = gameModelInstance.modelInstance
                    .transform.getTranslation(
                        auxVector3_1
                    )
                val distance =
                    enemyPosition.dst2(playerPosition)
                if (distance < closestDistance) {
                    val dot = enemyPosition.sub(playerPosition).nor().dot(playerDirection)
                    if (dot > 0.9) {
                        closestDistance = distance
                        closestEnemy = enemy
                    }
                }
            }
        }
        return closestEnemy
    }

    fun startPrimaryShooting() {
        priShooting = true
    }

    fun stopPrimaryShooting() {
        priShooting = false
    }

    fun startSecondaryShooting() {
        secShooting = true
    }

    fun stopSecondaryShooting() {
        secShooting = false
    }

    fun onTurretTouchPadTouchDown(deltaX: Float, deltaY: Float) {
        val dst = auxVector2.set(abs(deltaX), abs(deltaY)).dst2(Vector2.Zero)
        priShooting = dst >= 0.9
    }

    fun onTurretTouchPadTouchUp() {
        priShooting = false
    }

    companion object {
        private val auxVector2 = Vector2()
        private val auxVector3_1 = Vector3()
        private val auxVector3_2 = Vector3()
        private val auxVector3_3 = Vector3()
        private val auxMatrix = Matrix4()
        private val auxMatrix2 = Matrix4()
        private val auxQuat = Quaternion()
        private val auxQuat2 = Quaternion()
    }
}
