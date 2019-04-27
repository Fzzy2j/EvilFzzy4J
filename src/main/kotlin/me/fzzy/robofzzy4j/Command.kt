package me.fzzy.robofzzy4j

import me.fzzy.robofzzy4j.util.CommandCost
import me.fzzy.robofzzy4j.util.CommandResult
import sx.blah.discord.handle.obj.IMessage

interface Command {

    val description: String
    val votes: Boolean
    val usageText: String
    val allowDM: Boolean
    val cooldownMillis: Long
    val price: Int
    val cost: CommandCost

    fun runCommand(message: IMessage, args: List<String>): CommandResult

}