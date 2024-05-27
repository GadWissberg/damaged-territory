package com.gadarts.returnfire.systems.map

import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.systems.GameSessionData
import com.gadarts.returnfire.systems.SystemEvents

object MapUtils {
    fun notifyEntityRegionChanged(
        position: Vector3,
        prevRow: Int,
        prevColumn: Int,
        dispatcher: MessageDispatcher,
    ) {
        val newColumn = position.x.toInt() / GameSessionData.REGION_SIZE
        val newRow = position.z.toInt() / GameSessionData.REGION_SIZE
        if (prevColumn != newColumn || prevRow != newRow) {
            EntityEnteredNewRegionEventData.set(
                prevRow,
                prevColumn,
                newRow,
                newColumn
            )
            dispatcher.dispatchMessage(SystemEvents.ENTITY_ENTERED_NEW_REGION.ordinal)
        }
    }

}
