package com.gadarts.returnfire.ecs.components.ai

import com.badlogic.ashley.core.Component
import com.gadarts.returnfire.graph.MapGraphNode
import com.gadarts.returnfire.graph.MapGraphPath

class GroundCharacterAiComponent : Component {
    var roamingEndTime: Long? = null
    val nodesToExclude: MutableList<MapGraphNode> = ArrayList()
    var currentNode: MapGraphNode? = null
    val path: MapGraphPath = MapGraphPath()
    fun setNodesToExclude(nodesToExclude: List<MapGraphNode>) {
        this.nodesToExclude.clear()
        this.nodesToExclude.addAll(nodesToExclude)
    }

}
