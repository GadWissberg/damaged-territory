package com.gadarts.returnfire.systems.player.handlers

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.components.ArmComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.events.data.CharacterWeaponShotEventData
import kotlin.math.abs

class PlayerShootingHandler {
    private lateinit var player: Entity
    private lateinit var dispatcher: MessageDispatcher
    var secondaryCreationSide = false
    private var priShooting: Boolean = false
    private var secShooting: Boolean = false

    fun initialize(
        dispatcher: MessageDispatcher,
        player: Entity
    ) {

        this.player = player
        this.dispatcher = dispatcher
    }

    fun update() {
        var armComp: ArmComponent = ComponentsMapper.primaryArm.get(player)
        handleShooting(
            priShooting,
            armComp,
            SystemEvents.CHARACTER_WEAPON_ENGAGED_PRIMARY,
        )
        if (ComponentsMapper.secondaryArm.has(player)) {
            armComp = ComponentsMapper.secondaryArm.get(player)
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

        val now = TimeUtils.millis()
        if (armComp.loaded <= now) {
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
                        ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance.transform.getRotation(
                            auxQuat
                        )
                    auxMatrix.set(
                        rotation.setEulerAngles(rotation.yaw, 0F, 0F)
                    )
                } else {
                    val cannon =
                        ComponentsMapper.turret.get(ComponentsMapper.turretBase.get(player).turret).cannon
                    ComponentsMapper.modelInstance.get(cannon).gameModelInstance.modelInstance.transform
                }
            CharacterWeaponShotEventData.setWithDirection(
                player,
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
        private val auxMatrix = Matrix4()
        private val auxQuat = Quaternion()
    }
}
