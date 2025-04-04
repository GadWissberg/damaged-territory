package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.model.MapGraphPath
import com.gadarts.returnfire.model.graph.MapGraphNode
import com.gadarts.returnfire.systems.ai.AiStatus

class AiComponent : Component {
    val nodesToExclude: MutableList<MapGraphNode> = ArrayList()
    var currentNode: MapGraphNode? = null
    val path: MapGraphPath = MapGraphPath()
    var state: AiStatus = AiStatus.PLANNING
    var returnToBase: Boolean = false
        private set
    var lastHpCheck: Float = 0F
    val runAway = Vector3()
    var target: Entity? = null
    var attackReadyTime: Long = 0
    var attackReady: Boolean = true

    fun init(target: Entity?, initialHp: Float) {
        this.target = target
        this.lastHpCheck = initialHp
        this.attackReadyTime = 0
        this.attackReady = true
    }

    fun returnToBase() {
        returnToBase = true
    }

    fun setNodesToExclude(nodesToExclude: List<MapGraphNode>) {
        this.nodesToExclude.clear()
        this.nodesToExclude.addAll(nodesToExclude)
    }

}
