package com.gadarts.returnfire.systems.character

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.turret.TurretComponent

class TurretsHandler(engine: PooledEngine) {
    private val turretEntities: ImmutableArray<Entity> = engine.getEntitiesFor(
        Family.all(TurretComponent::class.java).get()
    )

    fun update() {
        for (turret in turretEntities) {
            val turretComponent = ComponentsMapper.turret.get(turret)
            val base = turretComponent.base
            if (turretComponent.followBase && ComponentsMapper.modelInstance.has(base)) {
                val baseTransform =
                    ComponentsMapper.modelInstance.get(base).gameModelInstance.modelInstance.transform
                baseTransform.getTranslation(auxVector1)
                val turretTransform =
                    ComponentsMapper.modelInstance.get(turret).gameModelInstance.modelInstance.transform
                turretTransform.setToTranslation(auxVector1).translate(auxVector2.set(0F, 0.2F, 0F))
                applyTurretOffsetFromBase(turretComponent, turretTransform)
                turretTransform.rotate(baseTransform.getRotation(auxQuat.idt()))
                turretTransform.rotate(Vector3.Y, turretComponent.turretRelativeRotation)
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
                    .translate(auxVector2.set(0.31F, 0F, 0F))
            }
        }
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
        private val auxQuat = com.badlogic.gdx.math.Quaternion()
    }
}
