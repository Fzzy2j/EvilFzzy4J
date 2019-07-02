package me.fzzy.robofzzy4j.command.help

import discord4j.core.`object`.entity.Message
import me.fzzy.robofzzy4j.Bot
import me.fzzy.robofzzy4j.command.Command
import me.fzzy.robofzzy4j.command.CommandCost
import me.fzzy.robofzzy4j.command.CommandResult
import reactor.core.publisher.Mono

object Invite : Command("invite") {

    override val cooldownMillis: Long = 4 * 1000
    override val votes: Boolean = false
    override val description = "Gives you the invite link for the bot to add it to servers"
    override val args: ArrayList<String> = arrayListOf()
    override val allowDM: Boolean = true
    override val price: Int = 0
    override val cost: CommandCost = CommandCost.CURRENCY

    override fun runCommand(message: Message, args: List<String>): Mono<CommandResult> {
        return message.author.get().privateChannel.flatMap { channel -> getInviteLink().flatMap { invite -> channel.createMessage(invite) } }
                .flatMap { Mono.just(CommandResult.success()) }
    }

    fun getInviteLink(): Mono<String> {
        return Bot.client.applicationInfo.flatMap { Mono.just("https://discordapp.com/oauth2/authorize?client_id=${it.id}&scope=bot&permissions=306240") }
    }

}