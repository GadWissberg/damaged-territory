package com.gadarts.returnfire.components.amb

import com.badlogic.ashley.core.Component
import com.gadarts.returnfire.assets.definitions.SoundDefinition

class AmbCorpsePart(val collisionSound: SoundDefinition?, val destroyOnGroundImpact: Boolean) : Component
