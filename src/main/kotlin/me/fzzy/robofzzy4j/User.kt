package me.fzzy.robofzzy4j

import sx.blah.discord.handle.obj.IUser

class User private constructor(val id: Long) {

    companion object {

        private val users: ArrayList<User> = arrayListOf()

        fun getUser(id: Long): User {
            for (user in users) {
                if (user.id == id)
                    return user
            }
            val user = User(id)
            users.add(user)
            return user
        }

        fun getUser(user: IUser): User {
            return getUser(user.longID)
        }
    }

    val cooldown = Cooldown()

    var runningCommand = false

}