package com.gadarts.returnfire.systems.ai.logic

import com.badlogic.gdx.ai.pfa.Heuristic
import com.badlogic.gdx.math.Vector2
import com.gadarts.returnfire.model.graph.MapGraphNode

class PathHeuristic : Heuristic<MapGraphNode> {
    override fun estimate(start: MapGraphNode, end: MapGraphNode): Float {
        return auxVector.set(start.x.toFloat(), start.y.toFloat()).dst2(end.x.toFloat(), end.y.toFloat())
    }

    companion object {
        private val auxVector = Vector2()
    }
}
