package com.gadarts.returnfire.components

class PlayerComponent : GameComponent() {
    private var fuel: Int = INITIAL_FUEL
    private var primaryAmmo: Int = INITIAL_AMMO_PRIMARY
    private var secondaryAmmo: Int = INITIAL_AMMO_SECONDARY

    override fun reset() {
    }


    fun init() {
        this.fuel = INITIAL_FUEL
        this.primaryAmmo = INITIAL_AMMO_PRIMARY
        this.secondaryAmmo = INITIAL_AMMO_SECONDARY
    }

    companion object {
        const val INITIAL_FUEL = 100
        const val INITIAL_AMMO_PRIMARY = 500
        const val INITIAL_AMMO_SECONDARY = 10
    }
}
