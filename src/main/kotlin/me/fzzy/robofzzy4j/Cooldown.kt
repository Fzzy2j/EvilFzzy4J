package me.fzzy.robofzzy4j

class Cooldown {

    private var cooldowns: HashMap<String, Long> = hashMapOf()

    fun triggerCooldown(key: String) {
        cooldowns[key] = System.currentTimeMillis()
    }

    fun getTimePassedMillis(key: String): Long {
        return System.currentTimeMillis() - cooldowns.getOrDefault(key, 0)
    }

    fun deleteCooldown(key: String) {
        cooldowns.remove(key)
    }

    fun clearCooldowns() {
        cooldowns.clear()
    }
}