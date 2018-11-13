package me.fzzy.robofzzy4j

class Cooldown {

    private var cooldown: Long = 0

    fun triggerCooldown() {
        cooldown = System.currentTimeMillis()
    }

    fun getTimePassedMillis(): Long {
        return System.currentTimeMillis() - cooldown
    }

    fun clearCooldown() {
        cooldown = 0
    }
}