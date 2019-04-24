package me.fzzy.robofzzy4j.util

class CommandResult private constructor(private val success: Boolean, private val message: String) {

    companion object {
        fun success(): CommandResult {
            return CommandResult(true, "")
        }

        fun fail(msg: String): CommandResult {
            return CommandResult(false, msg)
        }
    }

    fun isSuccess(): Boolean {
        return success
    }

    fun getFailMessage(): String {
        return message
    }

}