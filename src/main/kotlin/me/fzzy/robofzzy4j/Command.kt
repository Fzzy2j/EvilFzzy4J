package me.fzzy.robofzzy4j

import me.fzzy.robofzzy4j.util.CommandResult
import sx.blah.discord.handle.obj.IMessage

interface Command {

    val cooldownCategory: String
    val cooldownMillis: Long
    val description: String
    val votes: Boolean
    val usageText: String
    val allowDM: Boolean
    val cost: Int

    fun runCommand(message: IMessage, args: List<String>): CommandResult

}