package com.gadarts.returnfire.systems.ai

import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder
import com.gadarts.returnfire.managers.PathHeuristic
import com.gadarts.returnfire.model.MapGraphPath
import com.gadarts.returnfire.model.graph.GraphNode
import com.gadarts.returnfire.systems.data.GameSessionDataMap

class MapPathFinder(mapData: GameSessionDataMap, private val pathHeuristic: PathHeuristic) {
    private val pathFinder: IndexedAStarPathFinder<GraphNode> by lazy {
        IndexedAStarPathFinder<GraphNode>(mapData.mapGraph)
    }

    fun searchNodePath(start: GraphNode, end: GraphNode, path: MapGraphPath): Boolean {
        return pathFinder.searchNodePath(start, end, pathHeuristic, path)
    }

}
