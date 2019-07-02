package me.fzzy.robofzzy4j.command.voice

import discord4j.core.`object`.entity.Message
import me.fzzy.robofzzy4j.FzzyGuild
import me.fzzy.robofzzy4j.command.Command
import me.fzzy.robofzzy4j.command.CommandCost
import me.fzzy.robofzzy4j.command.CommandResult
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

    override fun runCommand(message: Message, args: List<String>): Mono<CommandResult> {

        FzzyGuild.getGuild(message.guild.block()!!.id).player.play(message.authorAsMember.block()!!.voiceState.block()!!.channel.block()!!, URL(args[0]), message.id)

        return Mono.just(CommandResult.success())
    }
}