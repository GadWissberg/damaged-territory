package com.gadarts.returnfire.managers

import com.badlogic.gdx.ai.pfa.Heuristic
import com.badlogic.gdx.math.Vector2
import com.gadarts.returnfire.model.graph.GraphNode

class PathHeuristic : Heuristic<GraphNode> {
    override fun estimate(start: GraphNode, end: GraphNode): Float {
        return auxVector.set(start.x.toFloat(), start.y.toFloat()).dst2(end.x.toFloat(), end.y.toFloat())
    }

    companion object {
        private val auxVector = Vector2()
    }
}
