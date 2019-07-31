package me.fzzy.evilfzzy4j.command.help

import me.fzzy.evilfzzy4j.command.Command
import me.fzzy.evilfzzy4j.command.CommandCost
import me.fzzy.evilfzzy4j.command.CommandResult
import reactor.core.publisher.Mono
import java.io.File

object Picturetypes : Command("picturetypes") {

    override val cooldownMillis: Long = 4 * 1000
    override val votes: Boolean = false
    override val description: String = "Shows all the picturess the bot can insert an image into using the -picture command"
    override val args: ArrayList<String> = arrayListOf()
    override val allowDM: Boolean = true
    override val price: Int = 0
    override val cost: CommandCost = CommandCost.CURRENCY

    override fun runCommand(message: CachedMessage, args: List<String>): Mono<CommandResult> {
        var all = "```"
        for (file in File("pictures").listFiles()!!) {
            all += "-picture ${file.nameWithoutExtension}\n"
        }
        all += "-picture random"
        all += "```"
        return message.author.privateChannel.flatMap { channel -> channel.createMessage(all).flatMap { Mono.just(CommandResult.success()) } }
    }

}