package com.gadarts.returnfire.components

import com.badlogic.gdx.math.Vector2

class PlayerComponent : GameComponent() {
    private val blastVelocity = Vector2()
    private var fuel: Int = INITIAL_FUEL
    private val currentVelocity = Vector2(1F, 0F)
    var strafing: Float? = null
    var primaryAmmo: Int = INITIAL_AMMO_PRIMARY
    var secondaryAmmo: Int = INITIAL_AMMO_SECONDARY

    override fun reset() {
    }

    fun getBlastVelocity(output: Vector2): Vector2 {
        return output.set(blastVelocity)
    }

    fun getCurrentVelocity(output: Vector2): Vector2 {
        return output.set(currentVelocity)
    }

    fun setCurrentVelocity(input: Vector2): Vector2 {
        currentVelocity.set(input)
        return input
    }

    fun init() {
        this.fuel = INITIAL_FUEL
        this.currentVelocity.set(1F, 0F)
        this.primaryAmmo = INITIAL_AMMO_PRIMARY
        this.secondaryAmmo = INITIAL_AMMO_SECONDARY
    }

    fun setBlastVelocity(velocity: Vector2) {
        blastVelocity.set(velocity)
    }

    companion object {
        const val INITIAL_FUEL = 100
        const val INITIAL_AMMO_PRIMARY = 500
        const val INITIAL_AMMO_SECONDARY = 10
    }
}
