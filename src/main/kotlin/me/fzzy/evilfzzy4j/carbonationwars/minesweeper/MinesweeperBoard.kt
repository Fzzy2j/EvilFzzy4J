package me.fzzy.evilfzzy4j.carbonationwars.minesweeper

import com.google.gson.annotations.Expose
import java.util.*

class MinesweeperBoard constructor(val sizeX: Int = 11, val sizeY: Int = 11, mineChance: Int = 10) {

    @Expose
    val board = Array(sizeX) { x -> Array(sizeY) { y -> Location(x, y) } }

    init {
        if (sizeX > 26) throw IllegalStateException("sizeX must not excede 26!")
        if (sizeY > 11) throw IllegalStateException("sizeY must not excede 11!")

        val random = Random()
        for (x in 0 until sizeX) {
            for (y in 0 until sizeY) {
                val r = random.nextInt(100)
                if (r < mineChance) board[x][y].isMine = true
            }
        }
    }

    private val numNames = arrayOf("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "keycap_ten")

    fun getBoardAsText(): String {
        val builder = StringBuilder()
        for (x in 0 until sizeX) {
            val grid = 'a' + x
            builder.append(":regional_indicator_$grid:")
        }
        builder.append("\n")
        for (y in 0 until sizeY) {
            for (x in 0 until sizeX) {
                builder.append(getTextRepresentation(x, y))
            }
            builder.append(":${numNames[y]}:\n")
        }
        return builder.toString()
    }

    fun isFinished(): Boolean {
        var notFinished = false
        for (y in 0 until sizeY) {
            for (x in 0 until sizeX) {
                if (!board[x][y].isAttacked && !board[x][y].isMine) notFinished = true
                if (board[x][y].isAttacked && board[x][y].isMine) return true
            }
        }
        return !notFinished
    }

    fun isWin(): Boolean {
        for (y in 0 until sizeY) {
            for (x in 0 until sizeX) {
                if (!board[x][y].isAttacked && !board[x][y].isMine) return false
            }
        }
        return true
    }

    fun whoAttacked(x: Char, y: Int): Long? {
        return try {
            board[x - 'a'][y].whoAttacked
        } catch (e: ArrayIndexOutOfBoundsException) {
            null
        }
    }

    fun attack(user: Long, x: Char, y: Int): Boolean {
        val actualX = x - 'a'
        return try {
            val success = board[actualX][y].attack(user)
            if (getAdjacentAmount(actualX, y) == 0) {
                for (xDif in -1..1) {
                    for (yDif in -1..1) {
                        if (xDif == 0 && yDif == 0) continue
                        try {
                            if (!board[actualX + xDif][y + yDif].isAttacked)
                                attack(user, x + xDif, y + yDif)
                        } catch (e: ArrayIndexOutOfBoundsException) {
                        }
                    }
                }
            }
            success
        } catch (e: ArrayIndexOutOfBoundsException) {
            false
        }
    }

    private fun getAdjacentAmount(x: Int, y: Int): Int {
        var count = 0
        for (x2 in -1..1) {
            for (y2 in -1..1) {
                try {
                    if ((x2 != 0 || y2 != 0) && board[x + x2][y + y2].isMine) count++
                } catch (e: ArrayIndexOutOfBoundsException) {
                }
            }
        }
        return count
    }

    fun getTextRepresentation(x: Int, y: Int): String {
        val loc = board[x][y]
        if (loc.isAttacked) {
            if (loc.isMine)
                return ":x:"
            val amt = getAdjacentAmount(x, y)
            return if (amt == 0)
                "⬛"
            else
                ":${numNames[amt]}:"
        }
        return if (!loc.isAttacked) "⬜" else if (loc.isMine) ":x:" else "⬛"
    }

    class Location constructor(@Expose val x: Int, @Expose val y: Int) {

        @Expose
        var isMine = false
        @Expose
        var isAttacked = false
            private set
        @Expose
        var whoAttacked: Long? = null
            private set

        fun attack(user: Long): Boolean {
            isAttacked = true
            whoAttacked = user
            return isMine
        }

    }
}