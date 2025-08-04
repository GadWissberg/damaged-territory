package com.gadarts.returnfire.systems.character

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.GreenComponent
import com.gadarts.returnfire.components.turret.TurretComponent
import com.gadarts.returnfire.utils.ModelUtils

class TurretsHandler(engine: PooledEngine) {
    private val turretEntities: ImmutableArray<Entity> = engine.getEntitiesFor(
        Family.all(TurretComponent::class.java).get()
    )
    private val greenCharactersEntities: ImmutableArray<Entity> = engine.getEntitiesFor(
        Family.all(GreenComponent::class.java).get()
    )

    fun update() {
        for (turret in turretEntities) {
            updateTurret(turret)
        }
    }

    private fun updateTurret(turret: Entity) {
        val turretComponent = ComponentsMapper.turret.get(turret)
        handleTurretAutomation(turret)
        val base = turretComponent.base
        if (turretComponent.followBase && ComponentsMapper.modelInstance.has(base)) {
            updateTurretTransform(base, turret)
        }
        val cannon = turretComponent.cannon
        if (cannon != null) {
            val turretTransform =
                ComponentsMapper.modelInstance.get(turret).gameModelInstance.modelInstance.transform
            turretTransform.getTranslation(
                auxVector1
            )
            ComponentsMapper.modelInstance.get(cannon).gameModelInstance.modelInstance.transform.setToTranslation(
                auxVector1
            ).rotate(turretTransform.getRotation(auxQuat.idt()))
                .translate(
                    auxVector2.set(
                        ComponentsMapper.turretCannonComponent.get(cannon).relativeX,
                        ComponentsMapper.turretCannonComponent.get(cannon).relativeY,
                        0F
                    )
                )
        }
    }

    private fun handleTurretAutomation(turret: Entity) {
        val turretAutomationComponent = ComponentsMapper.turretAutomationComponent.get(turret)
        if (turretAutomationComponent != null) {
            val target = turretAutomationComponent.target
            if (target == null) {
                var closestGreen: Entity? = null
                var closestGreenDistance = Float.MAX_VALUE
                val turretPosition = ModelUtils.getPositionOfModel(
                    turret,
                    auxVector1
                )
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
                }
            } else {
                val targetPosition = ModelUtils.getPositionOfModel(target)
                val turretTransform =
                    ComponentsMapper.modelInstance.get(turret).gameModelInstance.modelInstance.transform
                val directionToTarget = auxVector2.set(targetPosition).sub(
                    turretTransform.getTranslation(auxVector1)
                ).nor()
                directionToTarget.y = 0F
                val currentForward = auxVector4.setZero()
                auxVector3.set(turretTransform.getRotation(auxQuat).transform(Vector3.Z)).also {
                    currentForward.set(it.x, 0f, it.z).nor()
                }
//                val angleRad = currentForward.angleRad(directionToTarget)
//                val crossY = currentForward.crs(flatDirectionToTarget).y
//                val signedAngleRad = if (crossY >= 0f) angleRad else -angleRad
//
//// 4. Apply rotation around Y axis
//                val rotationQuat = Quaternion(Vector3.Y, signedAngleRad * MathUtils.radiansToDegrees)
//                turretTransform.rotate(rotationQuat)

            }
        }
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
        turretTransform.setToTranslation(auxVector1)
            .translate(auxVector2.set(0F, turretComponent.relativeHeight, 0F))
        applyTurretOffsetFromBase(turretComponent, turretTransform)
        turretTransform.rotate(baseTransform.getRotation(auxQuat.idt()))
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
        private val auxQuat = com.badlogic.gdx.math.Quaternion()
        private val auxMatrix = Matrix4()
    }
}
