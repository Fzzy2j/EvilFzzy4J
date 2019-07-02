package me.fzzy.robofzzy4j.command

class Cooldown {

    private var cooldownStamp: Long = 0
    private var cooldown: Long = 0

    fun triggerCooldown(time: Long) {
        cooldown = time
        cooldownStamp = System.currentTimeMillis()
    }

    fun timeLeft(scale: Double): Long {
        return (cooldownStamp + (cooldown * scale).toLong()) - System.currentTimeMillis()
    }

    fun isReady(scale: Double): Boolean {
        return timeLeft(scale) <= 0
    }

    fun clearCooldown() {
        cooldown = 0
    }
}