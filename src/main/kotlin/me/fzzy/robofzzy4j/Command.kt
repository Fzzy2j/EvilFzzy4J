package me.fzzy.robofzzy4j

import me.fzzy.robofzzy4j.util.CommandCost
import me.fzzy.robofzzy4j.util.CommandResult
import sx.blah.discord.handle.obj.IMessage

abstract class Command constructor(val name: String) {

    abstract val description: String
    abstract val votes: Boolean
    abstract val args: ArrayList<String>
    abstract val allowDM: Boolean
    abstract val cooldownMillis: Long
    abstract val price: Int
    abstract val cost: CommandCost

    abstract fun runCommand(message: IMessage, args: List<String>): CommandResult

}