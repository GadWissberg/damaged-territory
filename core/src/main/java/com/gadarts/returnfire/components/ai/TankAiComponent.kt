package com.gadarts.returnfire.components.ai

import com.badlogic.ashley.core.Component
import com.gadarts.returnfire.model.MapGraphPath
import com.gadarts.returnfire.model.graph.MapGraphNode

class TankAiComponent : Component {
    var roamingEndTime: Long? = null
    val nodesToExclude: MutableList<MapGraphNode> = ArrayList()
    var currentNode: MapGraphNode? = null
    val path: MapGraphPath = MapGraphPath()
    fun setNodesToExclude(nodesToExclude: List<MapGraphNode>) {
        this.nodesToExclude.clear()
        this.nodesToExclude.addAll(nodesToExclude)
    }

}
