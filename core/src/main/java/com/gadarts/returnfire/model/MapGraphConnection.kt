package com.gadarts.returnfire.model

import com.badlogic.gdx.ai.pfa.Connection
import com.gadarts.returnfire.model.graph.MapGraphNode

class MapGraphConnection(private val from: MapGraphNode, private val to: MapGraphNode, private val cost: MapGraphCost) :
    Connection<MapGraphNode> {
    override fun getCost(): Float {
        return cost.ordinal.toFloat()
    }

    override fun getFromNode(): MapGraphNode {
        return from
    }

    override fun getToNode(): MapGraphNode {
        return to
    }

}
