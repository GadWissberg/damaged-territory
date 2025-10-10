package com.gadarts.returnfire.ecs.components.arm

import com.badlogic.ashley.core.Entity
import com.gadarts.returnfire.ecs.components.bullet.BulletBehavior

class PrimaryArmComponent(
    armProperties: ArmProperties, spark: Entity, bulletBehavior: BulletBehavior,ammo:Int
) : ArmComponent(
    armProperties, spark, bulletBehavior,ammo
) {
    override fun isPrimary(): Boolean {
        return true
    }
}
