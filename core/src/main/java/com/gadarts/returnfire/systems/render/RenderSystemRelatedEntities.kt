package com.gadarts.returnfire.systems.render

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray

class RenderSystemRelatedEntities(
    val modelInstanceEntities: ImmutableArray<Entity>,
    val armEntities: ImmutableArray<Entity>,
    val childEntities: ImmutableArray<Entity>,
    val decalEntities: ImmutableArray<Entity>
)
