package me.fzzy.robofzzy4j

import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.MessageChannel
import reactor.core.publisher.Mono
import java.util.*
import java.util.concurrent.TimeUnit

object MessageScheduler {

    private var tempMessages: HashMap<Message, Long> = hashMapOf()

    init {
        Bot.scheduler.schedulePeriodically({
            val iter = tempMessages.iterator()
            while (iter.hasNext()) {
                val i = iter.next()
                if (System.currentTimeMillis() > i.value) {
                    i.key.delete()
                    iter.remove()
                }
            }
        }, 1, 1, TimeUnit.SECONDS)
    }

    fun clearTempMessage(message: Message) {
        val iter = tempMessages.iterator()
        while (iter.hasNext()) {
            val i = iter.next()
            if (message == i.key) {
                iter.remove()
                break
            }
        }
    }

    fun sendTempMessage(channel: MessageChannel, text: String, timeToStaySeconds: Long): Mono<Message> {
        val mono = channel.createMessage(text)

        mono.doOnSuccess { tempMessages[it] = System.currentTimeMillis() + TimeUnit.SECONDS.convert(timeToStaySeconds, TimeUnit.MILLISECONDS) }

        return mono
    }
}