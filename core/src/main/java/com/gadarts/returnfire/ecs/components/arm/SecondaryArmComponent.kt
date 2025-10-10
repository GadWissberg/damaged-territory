package com.gadarts.returnfire.ecs.components.arm

import com.badlogic.ashley.core.Entity
import com.gadarts.returnfire.ecs.components.bullet.BulletBehavior

class SecondaryArmComponent(
    armProperties: ArmProperties, spark: Entity, bulletBehavior: BulletBehavior,ammo:Int
) : ArmComponent(
    armProperties, spark, bulletBehavior,ammo
) {
    fun flipCreationSide() {
        creationSide *= -1
    }

    var creationSide: Int = LEFT
        private set

    companion object {
        private const val LEFT = 1
    }

    override fun isPrimary(): Boolean {
        return false
    }
}

