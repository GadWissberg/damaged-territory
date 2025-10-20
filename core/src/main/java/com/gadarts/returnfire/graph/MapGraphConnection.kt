package com.gadarts.returnfire.graph

import com.badlogic.gdx.ai.pfa.Connection

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
