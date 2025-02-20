package com.gadarts.returnfire.systems.render

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray

class RenderSystemRelatedEntities(
    val modelInstanceEntities: ImmutableArray<Entity>,
    val childEntities: ImmutableArray<Entity>,
    val decalEntities: ImmutableArray<Entity>,
    val waterWaveEntities: ImmutableArray<Entity>,
    val animationEntities: ImmutableArray<Entity>,
) {
    val independentDecalsToRemove: ArrayList<Entity> = ArrayList()
}
