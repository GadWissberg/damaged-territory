package com.gadarts.returnfire.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation

class AnimatedTextureComponent(val animation: Animation<Texture>) : Component
