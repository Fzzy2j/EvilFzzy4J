package me.fzzy.robofzzy4j

class Cooldown {

    private var cooldownStamp: Long = 0
    private var cooldown: Long = 0

    fun triggerCooldown(time: Long) {
        cooldown = time
        cooldownStamp = System.currentTimeMillis()
    }

    fun timeLeft(scale: Double): Long {
        return System.currentTimeMillis() - cooldownStamp + (cooldown * scale).toLong()
    }

    fun isReady(scale: Double): Boolean {
        return System.currentTimeMillis() > cooldownStamp + (cooldown * scale)
    }

    fun clearCooldown() {
        cooldown = 0
    }
}