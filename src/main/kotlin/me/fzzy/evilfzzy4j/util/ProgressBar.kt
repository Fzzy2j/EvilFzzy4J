package me.fzzy.evilfzzy4j.util

object ProgressBar {

    private val text = "░▒▓█"

    fun getBar(percentage: Int): String {
        val builder = StringBuilder()

        if (percentage >= 100) {
            for (i in 0 until 25)
                builder.append(text[3])
            return builder.toString()
        }

        val full = percentage / 4
        for (i in 0 until full) {
            builder.append(text[3])
        }
        val between = percentage % 4
        builder.append(text[between])
        for (i in 0 until 25 - full - 1) {
            builder.append(text[0])
        }
        return builder.toString()
    }

}