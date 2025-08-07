package com.gadarts.returnfire.systems.character

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.GreenComponent
import com.gadarts.returnfire.components.TurretAutomationComponent
import com.gadarts.returnfire.components.turret.TurretComponent
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.physics.BulletEngineHandler
import com.gadarts.returnfire.utils.ModelUtils
import com.gadarts.shared.assets.definitions.SoundDefinition
import kotlin.math.sqrt

class TurretsHandler(gamePlayManagers: GamePlayManagers, gameSessionData: GameSessionData) {
    private val shootingHandler = CharacterShootingHandler(
        gamePlayManagers.ecs.entityBuilder,
        gamePlayManagers.soundManager,
        gamePlayManagers.assetsManager.getAssetByDefinition(
            SoundDefinition.EMPTY
        )
    )

    init {
        shootingHandler.initialize(
            gamePlayManagers.dispatcher, gameSessionData, gamePlayManagers.factories.autoAimShapeFactory.generate(
                BulletEngineHandler.COLLISION_GROUP_PLAYER,
                BulletEngineHandler.COLLISION_GROUP_AI,
            )
        )
    }

    private val turretEntities: ImmutableArray<Entity> = gamePlayManagers.ecs.engine.getEntitiesFor(
        Family.all(TurretComponent::class.java).get()
    )
    private val greenCharactersEntities: ImmutableArray<Entity> = gamePlayManagers.ecs.engine.getEntitiesFor(
        Family.all(GreenComponent::class.java).get()
    )

    fun update(deltaTime: Float) {
        for (turret in turretEntities) {
            updateTurret(turret, deltaTime)
        }
    }

    private fun updateTurret(turret: Entity, deltaTime: Float) {
        val turretComponent = ComponentsMapper.turret.get(turret)
        val base = turretComponent.base
        val turretTransform =
            ComponentsMapper.modelInstance.get(turret).gameModelInstance.modelInstance.transform
        if (turretComponent.followBaseRotation && ComponentsMapper.modelInstance.has(base)) {
            updateTurretTransform(base, turret)
        }
        if (turretComponent.followBasePosition) {
            val baseTransform =
                ComponentsMapper.modelInstance.get(base).gameModelInstance.modelInstance.transform
            baseTransform.getTranslation(auxVector1)
            turretTransform.setTranslation(auxVector1).translate(auxVector2.set(0F, turretComponent.relativeHeight, 0F))
        }
        applyTurretOffsetFromBase(turretComponent, turretTransform)
        handleTurretAutomation(turret, deltaTime)
        val cannon = turretComponent.cannon
        if (cannon != null) {
            turretTransform.getTranslation(
                auxVector1
            )
            val cannonTransform = ComponentsMapper.modelInstance.get(cannon).gameModelInstance.modelInstance.transform
            val cannonRotation = cannonTransform.getRotation(auxQuat2)
            cannonTransform.setToTranslation(
                auxVector1
            ).rotate(turretTransform.getRotation(auxQuat1.idt())).rotate(Vector3.Z, cannonRotation.roll)
                .trn(
                    auxVector2.set(
                        ComponentsMapper.turretCannonComponent.get(cannon).relativeX,
                        ComponentsMapper.turretCannonComponent.get(cannon).relativeY,
                        0F
                    )
                )
        }
    }

    private fun handleTurretAutomation(turret: Entity, deltaTime: Float) {
        val turretAutomationComponent = ComponentsMapper.turretAutomationComponent.get(turret) ?: return
        val turretComponent = ComponentsMapper.turret.get(turret)

        val target = turretAutomationComponent.target
        val turretPosition = ModelUtils.getPositionOfModel(turret, auxVector1)
        if (target == null) {
            findClosestEnemy(turretPosition, turretAutomationComponent, turretComponent)
        } else {
            if (ModelUtils.getPositionOfModel(target).dst2(turretPosition) > AUTOMATED_TURRET_MAX_DISTANCE) {
                turretComponent.followBaseRotation = true
                turretAutomationComponent.target = null
            } else {
                val aimedHorizontally = rotateAutomatedTurret(turret, deltaTime)
                if (aimedHorizontally) {
                    val targetPosition = ModelUtils.getPositionOfModel(target, auxVector2)
                    val directionToTarget = targetPosition.sub(turretPosition).nor()
                    val targetElevationAngle = MathUtils.atan2(
                        directionToTarget.y,
                        sqrt(directionToTarget.x * directionToTarget.x + directionToTarget.z * directionToTarget.z)
                    )
                    val targetElevationDegrees = targetElevationAngle * MathUtils.radiansToDegrees
                    val transform =
                        ComponentsMapper.modelInstance.get(turretComponent.cannon).gameModelInstance.modelInstance.transform
                    transform.setToRotation(Vector3.Z, targetElevationDegrees)
                    if (!shootingHandler.isPrimaryShooting()) {
                        shootingHandler.startPrimaryShooting(turretComponent.base)
                    }
                } else {
                    shootingHandler.stopPrimaryShooting()
                }
            }
        }
        shootingHandler.update(turretComponent.base)
    }

    private fun findClosestEnemy(
        turretPosition: Vector3,
        turretAutomationComponent: TurretAutomationComponent,
        turretComponent: TurretComponent
    ) {
        var closestGreen: Entity? = null
        var closestGreenDistance = AUTOMATED_TURRET_MAX_DISTANCE
        for (greenCharacter in greenCharactersEntities) {
            val greenCharacterComponent = ComponentsMapper.character.get(greenCharacter)
            if (!greenCharacterComponent.dead && greenCharacterComponent.hp > 0) {
                val greenPosition = ModelUtils.getPositionOfModel(
                    greenCharacter,
                    auxVector2
                )
                if (greenPosition.dst2(turretPosition) < closestGreenDistance) {
                    closestGreenDistance = greenPosition.dst2(turretPosition)
                    closestGreen = greenCharacter
                }
            }
        }
        if (closestGreen != null) {
            turretAutomationComponent.target = closestGreen
            turretComponent.followBaseRotation = false
        } else {
            shootingHandler.stopPrimaryShooting()
        }
    }

    private fun rotateAutomatedTurret(
        turret: Entity,
        deltaTime: Float
    ): Boolean {
        val turretAutomationComponent = ComponentsMapper.turretAutomationComponent.get(turret)
        val target = turretAutomationComponent.target ?: return false

        val targetPosition = ModelUtils.getPositionOfModel(target)
        val turretInstance = ComponentsMapper.modelInstance.get(turret).gameModelInstance.modelInstance
        val turretTransform = turretInstance.transform
        val toTarget = targetPosition.sub(turretTransform.getTranslation(auxVector1))
        val directionToTarget = auxVector4.set(toTarget.x, 0f, toTarget.z).nor()
        val forward = turretTransform.getRotation(auxQuat1).transform(auxVector2.set(Vector3.X)).nor()
        val currentForwardFlat = auxVector3.set(forward.x, 0f, forward.z).nor()
        val dot = currentForwardFlat.dot(directionToTarget)
        val clampedDot = MathUtils.clamp(dot, -1f, 1f)
        val angleDeg = MathUtils.acos(clampedDot) * MathUtils.radiansToDegrees
        if (angleDeg > 2f) {
            val cross = currentForwardFlat.crs(directionToTarget)
            val signedAngleDeg = if (cross.y >= 0f) angleDeg else -angleDeg
            val maxTurnDegPerFrame = 90f * deltaTime
            val clampedDeg = MathUtils.clamp(signedAngleDeg, -maxTurnDegPerFrame, maxTurnDegPerFrame)
            val rotationQuat = auxQuat1.set(Vector3.Y, clampedDeg)
            turretTransform.rotate(rotationQuat)
            return false
        }
        return true
    }

    private fun updateTurretTransform(
        base: Entity,
        turret: Entity?,
    ) {
        val turretComponent = ComponentsMapper.turret.get(turret)
        val baseTransform =
            ComponentsMapper.modelInstance.get(base).gameModelInstance.modelInstance.transform
        baseTransform.getTranslation(auxVector1)
        val turretTransform =
            ComponentsMapper.modelInstance.get(turret).gameModelInstance.modelInstance.transform
        turretTransform.idt()
        turretTransform.rotate(baseTransform.getRotation(auxQuat1.idt()))
        turretTransform.rotate(Vector3.Y, turretComponent.turretRelativeRotation)
    }

    private fun applyTurretOffsetFromBase(
        turretComponent: TurretComponent,
        turretTransform: Matrix4,
    ) {
        if (turretComponent.baseOffsetApplied) {
            val offset = turretComponent.getBaseOffset(auxVector3)
            turretTransform.translate(offset)
            offset.lerp(Vector3.Zero, 0.05F)
            turretComponent.setBaseOffset(offset)
            if (offset.epsilonEquals(Vector3.Zero)) {
                turretComponent.baseOffsetApplied = false
            }
        }
    }

    companion object {
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxVector3 = Vector3()
        private val auxVector4 = Vector3()
        private val auxQuat1 = com.badlogic.gdx.math.Quaternion()
        private val auxQuat2 = com.badlogic.gdx.math.Quaternion()
        private const val AUTOMATED_TURRET_MAX_DISTANCE = 60F
    }
}
