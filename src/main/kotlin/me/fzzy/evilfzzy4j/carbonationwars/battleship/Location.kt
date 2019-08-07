package me.fzzy.evilfzzy4j.carbonationwars.battleship

import com.google.gson.annotations.Expose
import discord4j.core.`object`.entity.User

class Location constructor(@Expose val x: Int, @Expose val y: Int) {

    @Expose
    var isShip = false
    @Expose
    var isAttacked = false
        private set
    @Expose
    var whoAttacked: User? = null
        private set

    fun attack(user: User): Boolean {
        isAttacked = true
        whoAttacked = user
        return isShip
    }

    fun getTextRepresentation(): String {
        return if (!isAttacked) "⬜" else if (isShip) ":x:" else "⬛"
    }

}