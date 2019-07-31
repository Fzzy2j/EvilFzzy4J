package me.fzzy.evilfzzy4j

import discord4j.core.`object`.util.Snowflake
import me.fzzy.evilfzzy4j.command.Cooldown

class FzzyUser private constructor(val id: Snowflake) {

    companion object {

        private val users: ArrayList<FzzyUser> = arrayListOf()

        fun getUser(id: Snowflake): FzzyUser {
            for (user in users) {
                if (user.id == id)
                    return user
            }
            val user = FzzyUser(id)
            users.add(user)
            return user
        }

        fun getUser(user: FzzyUser): FzzyUser {
            return getUser(user.id)
        }
    }

    val cooldown = Cooldown()

}