package com.gadarts.dte.scene.handlers

import com.badlogic.gdx.ai.msg.Telegram

interface EditorOnEvent {
    fun react(msg: Telegram)

}
