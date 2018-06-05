package me.fzzy.eventvoter

class Cooldown {

    private var cooldowns: HashMap<String, Long> = hashMapOf()

    fun triggerCooldown(key: String) {
        cooldowns[key] = System.currentTimeMillis()
    }

    fun getTimePassedMillis(key: String): Long {
        return System.currentTimeMillis() - cooldowns.getOrDefault(key, 0)
    }

}