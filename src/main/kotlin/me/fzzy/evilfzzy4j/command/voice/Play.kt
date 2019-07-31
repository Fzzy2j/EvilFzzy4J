package me.fzzy.evilfzzy4j.command.voice

import me.fzzy.evilfzzy4j.FzzyGuild
import me.fzzy.evilfzzy4j.command.Command
import me.fzzy.evilfzzy4j.command.CommandCost
import me.fzzy.evilfzzy4j.command.CommandResult
import reactor.core.publisher.Mono
import java.net.URL


object Play : Command("play") {

    override val cooldownMillis: Long = 1000 * 60 * 10
    override val votes: Boolean = true
    override val description = "Plays audio in the voice channel"
    override val args: ArrayList<String> = arrayListOf("url")
    override val allowDM: Boolean = true
    override val price: Int = 4
    override val cost: CommandCost = CommandCost.CURRENCY

    override fun runCommand(message: CachedMessage, args: List<String>): Mono<CommandResult> {

        FzzyGuild.getGuild(message.guild.id).player.play(message.authorAsMember.voiceState.block()!!.channel.block()!!, URL(args[0]), message.original!!.id)

        return Mono.just(CommandResult.success())
    }
}