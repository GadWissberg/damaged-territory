package com.gadarts.returnfire.systems.character

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btCollisionObjectArray
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
import kotlin.math.min

open class CharacterShootingHandler(private val entityBuilder: EntityBuilder) {
    protected var priShooting: Boolean = false
    private var secShooting: Boolean = false
    protected lateinit var gameSessionData: GameSessionData
    protected lateinit var dispatcher: MessageDispatcher
    protected var autoAim: btPairCachingGhostObject? = null
    protected var aimSky: Boolean = false

    fun startPrimaryShooting() {
        priShooting = true
    }

    private fun handleShooting(
        shooting: Boolean,
        armComp: ArmComponent,
        event: SystemEvents,
        character: Entity
    ) {
        if (!shooting) return

        val now = TimeUtils.millis()
        if (armComp.loaded <= now) {
            val modelInstanceComponent = ComponentsMapper.modelInstance.get(character)
            val transform = modelInstanceComponent.gameModelInstance.modelInstance.transform
            armComp.displaySpark = now
            armComp.loaded = now + armComp.armProperties.reloadDuration
            val direction =
                if (!ComponentsMapper.turretBase.has(character) || ComponentsMapper.turret.get(
                        ComponentsMapper.turretBase.get(
                            character
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
                        ComponentsMapper.turret.get(ComponentsMapper.turretBase.get(character).turret).cannon
                    val direction = ComponentsMapper.modelInstance.get(cannon).gameModelInstance.modelInstance.transform
                    if (armComp.isPrimary()) {
                        val particleEffect = entityBuilder.begin().addParticleEffectComponent(
                            direction.getTranslation(auxVector3_1),
                            gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE_SMALL),
                            parentRelativePosition = auxVector3_2.set(0.5F, 0F, 0F),
                        ).finishAndAddToEngine()
                        ComponentsMapper.particleEffect.get(particleEffect).parent = cannon
                    }
                    direction
                }
            val target = handleAutoAim(transform)
            if (target == null) {
                CharacterWeaponShotEventData.setWithDirection(
                    character,
                    direction,
                    aimSky
                )
            } else {
                CharacterWeaponShotEventData.setWithTarget(character, target)
            }
            dispatcher.dispatchMessage(event.ordinal)
        }
    }

    private fun updateAutoAim(character: Entity) {
        if (autoAim == null) return

        val armComp: ArmComponent = ComponentsMapper.primaryArm.get(gameSessionData.gamePlayData.player)
        val rigidBody = autoAim
        val turretBaseComponent = ComponentsMapper.turretBase.get(character)
        val playerModelInstance =
            if (turretBaseComponent != null) {
                ComponentsMapper.modelInstance.get(turretBaseComponent.turret).gameModelInstance.modelInstance
            } else {
                ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance
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
        rigidBody!!.worldTransform = autoAimTransform
    }

    private fun handleAutoAim(transform: Matrix4): Entity? {
        if (autoAim == null) return null

        val overlappingPairs = autoAim!!.overlappingPairs
        val size = min(overlappingPairs.size(), 6)
        var closestCharacter: Entity? = null
        if (size > 0) {
            closestCharacter = findClosestCharacter(transform, size, overlappingPairs)
        }
        return closestCharacter
    }

    private fun findClosestCharacter(
        transform: Matrix4,
        size: Int,
        overlappingPairs: btCollisionObjectArray
    ): Entity? {
        var closestCharacter: Entity? = null
        val playerPosition = transform.getTranslation(auxVector3_2)
        var closestDistance = Float.MAX_VALUE
        for (i in 0 until size) {
            val character = overlappingPairs.atConst(i).userData
            if (character != null) {
                val gameModelInstance = ComponentsMapper.modelInstance.get(character as Entity).gameModelInstance
                val enemyPosition = gameModelInstance.modelInstance
                    .transform.getTranslation(
                        auxVector3_1
                    )
                val distance =
                    enemyPosition.dst2(playerPosition)
                if (distance < closestDistance) {
                    val dot =
                        enemyPosition.set(enemyPosition.x, playerPosition.y, enemyPosition.z).sub(playerPosition)
                            .nor()
                            .dot(
                                transform.getRotation(auxQuat)
                                    .transform(auxVector3_3.set(Vector3.X))
                            )
                    if (dot > 0.85) {
                        closestDistance = distance
                        closestCharacter = character
                    }
                }
            }
        }
        return closestCharacter
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

    open fun update(character: Entity) {
        updateAutoAim(character)
        var armComp: ArmComponent = ComponentsMapper.primaryArm.get(character)
        handleShooting(
            priShooting,
            armComp,
            SystemEvents.CHARACTER_WEAPON_ENGAGED_PRIMARY,
            character
        )
        if (ComponentsMapper.secondaryArm.has(character)) {
            armComp = ComponentsMapper.secondaryArm.get(character)
            handleShooting(
                secShooting,
                armComp,
                SystemEvents.CHARACTER_WEAPON_ENGAGED_SECONDARY,
                character
            )
        }
    }

    open fun initialize(
        dispatcher: MessageDispatcher,
        gameSessionData: GameSessionData,
        autoAim: btPairCachingGhostObject?
    ) {
        this.gameSessionData = gameSessionData
        this.dispatcher = dispatcher
        this.autoAim = autoAim
    }

    companion object {
        private val auxQuat = Quaternion()
        private val auxQuat2 = Quaternion()
        private val auxMatrix = Matrix4()
        private val auxMatrix2 = Matrix4()
        private val auxVector3_1 = Vector3()
        private val auxVector3_2 = Vector3()
        private val auxVector3_3 = Vector3()
    }
}
