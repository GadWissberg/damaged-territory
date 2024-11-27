package com.gadarts.returnfire.components.arm

import com.badlogic.ashley.core.Entity
import com.gadarts.returnfire.components.bullet.BulletBehavior

class PrimaryArmComponent(
    armProperties: ArmProperties, spark: Entity, bulletBehavior: BulletBehavior
) : ArmComponent(
    armProperties, spark, bulletBehavior
)
