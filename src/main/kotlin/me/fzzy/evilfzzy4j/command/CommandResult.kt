package me.fzzy.evilfzzy4j.command

class CommandResult private constructor(private val success: Boolean, private val message: String?) {

    companion object {
        fun success(msg: String? = null): CommandResult {
            return CommandResult(true, msg)
        }

        fun fail(msg: String? = null): CommandResult {
            return CommandResult(false, msg)
        }
    }

    fun isSuccess(): Boolean {
        return success
    }

    fun getMessage(): String? {
        return message
    }

}