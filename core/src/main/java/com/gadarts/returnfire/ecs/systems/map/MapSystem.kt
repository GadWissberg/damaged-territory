package com.gadarts.returnfire.ecs.systems.map

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector3
import com.gadarts.shared.assets.definitions.model.ModelDefinition

interface MapSystem {
    fun destroyAmbObject(amb: Entity)
    fun destroyTree(
        tree: Entity,
        decorateWithSmokeAndFire: Boolean
    )

    fun findBase(entity: Entity): Entity
    fun closeDoors(base: Entity)
    fun hideLandingMark()
    fun blowAmbToParts(
        position: Vector3,
        modelDefinition: ModelDefinition,
        min: Int,
        max: Int,
        gravityScale: Float,
        decorateWithSmokeAndFire: Boolean,
        minImpulse: Float,
        maxImpulse: Float,
        positionBiasMax: Float
    )
}
