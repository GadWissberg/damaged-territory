package com.gadarts.returnfire.utils

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.utils.GeneralUtils.auxVector1

object ModelUtils {

    fun getPositionOfModel(entity: Entity): Vector3 {
        return getPositionOfModel(entity, auxVector1)
    }

    fun getPositionOfModel(entity: Entity, output: Vector3): Vector3 {
        return ComponentsMapper.modelInstance.get(entity).gameModelInstance.modelInstance.transform.getTranslation(
            output
        )
    }


}
