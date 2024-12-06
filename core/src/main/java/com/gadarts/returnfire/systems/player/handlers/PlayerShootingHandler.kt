package com.gadarts.returnfire.systems.player.handlers

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.arm.ArmComponent
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.events.data.CharacterWeaponShotEventData
import kotlin.math.abs

class PlayerShootingHandler(private val entityBuilder: EntityBuilder) {
    private lateinit var autoAim: Entity
    private lateinit var gameSessionData: GameSessionData
    private lateinit var dispatcher: MessageDispatcher
    var secondaryCreationSide = false
    private var priShooting: Boolean = false
    private var secShooting: Boolean = false

    fun initialize(
        dispatcher: MessageDispatcher,
        gameSessionData: GameSessionData,
        autoAim: Entity,
    ) {
        this.gameSessionData = gameSessionData
        this.dispatcher = dispatcher
        this.autoAim = autoAim
    }

    fun update() {
        var armComp: ArmComponent = ComponentsMapper.primaryArm.get(gameSessionData.gameplayData.player)
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

    private fun handleShooting(
        shooting: Boolean,
        armComp: ArmComponent,
        event: SystemEvents,
    ) {
        if (!shooting) return

        val modelInstanceComponent = ComponentsMapper.modelInstance.get(gameSessionData.gameplayData.player)
        val transform = modelInstanceComponent.gameModelInstance.modelInstance.transform
        val rigidBody = ComponentsMapper.physics.get(autoAim).rigidBody
        rigidBody.worldTransform = rigidBody.worldTransform.set(auxMatrix.set(transform).rotate(Vector3.Z, 90F))
        val now = TimeUtils.millis()
        if (armComp.loaded <= now) {
            armComp.displaySpark = now
            armComp.loaded = now + armComp.armProperties.reloadDuration
            val direction =
                if (!ComponentsMapper.turretBase.has(gameSessionData.gameplayData.player) || ComponentsMapper.turret.get(
                        ComponentsMapper.turretBase.get(
                            gameSessionData.gameplayData.player
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
                        ComponentsMapper.turret.get(ComponentsMapper.turretBase.get(gameSessionData.gameplayData.player).turret).cannon
                    val direction = ComponentsMapper.modelInstance.get(cannon).gameModelInstance.modelInstance.transform
                    val particleEffect = entityBuilder.begin().addParticleEffectComponent(
                        direction.getTranslation(auxVector3_1),
                        gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE_SMALL),
                        parentRelativePosition = auxVector3_2.set(0.5F, 0F, 0F),
                    ).finishAndAddToEngine()
                    ComponentsMapper.particleEffect.get(particleEffect).parent = cannon
                    direction
                }
            CharacterWeaponShotEventData.setWithDirection(
                gameSessionData.gameplayData.player,
                direction
            )
            dispatcher.dispatchMessage(event.ordinal)
        }
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
        private val auxMatrix = Matrix4()
        private val auxQuat = Quaternion()
    }
}
