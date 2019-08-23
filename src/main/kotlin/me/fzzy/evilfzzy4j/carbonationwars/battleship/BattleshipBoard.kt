package me.fzzy.evilfzzy4j.carbonationwars.battleship

import com.google.gson.annotations.Expose
import discord4j.core.`object`.entity.User
import java.util.*

class BattleshipBoard constructor(val sizeX: Int = 11, val sizeY: Int = 11, numBoats: Int = 5) {

    @Expose
    val board = Array(sizeX) { x -> Array(sizeY) { y -> Location(x, y) } }

    init {
        if (sizeX > 26) throw IllegalStateException("sizeX must not excede 26!")
        if (sizeY > 11) throw IllegalStateException("sizeY must not excede 11!")

        val random = Random()
        for (i in 0 until numBoats) {
            fun isValid(directionX: Int, directionY: Int, startX: Int, startY: Int, length: Int): Boolean {
                for (a in 0 until length) {
                    try {
                        if (board[startX + directionX * a][startY + directionY * a].isShip)
                            return false
                    } catch (e: ArrayIndexOutOfBoundsException) {
                        return false
                    }
                }
                return true
            }

            var startX: Int
            var startY: Int
            var directionX: Int
            var directionY: Int
            var length: Int
            do {
                directionX = 0
                directionY = 0
                when (random.nextInt(4)) {
                    0 -> directionX = 1
                    1 -> directionX = -1
                    2 -> directionY = 1
                    3 -> directionY = -1
                }

                startX = random.nextInt(sizeX)
                startY = random.nextInt(sizeY)

                length = random.nextInt(4) + 2
            } while (!isValid(directionX, directionY, startX, startY, length))

            for (a in 0 until length) {
                board[startX + directionX * a][startY + directionY * a].isShip = true
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
                builder.append(board[x][y].getTextRepresentation())
            }
            builder.append(":${numNames[y]}:\n")
        }
        return builder.toString()
    }

    fun allAttacked(): Boolean {
        for (y in 0 until sizeY) {
            for (x in 0 until sizeX) {
                if (!board[x][y].isAttacked && board[x][y].isShip) return false
            }
        }
        return true
    }

    fun whoAttacked(x: Char, y: Int): User? {
        return try {
            board[x - 'a'][y].whoAttacked
        } catch (e: ArrayIndexOutOfBoundsException) {
            null
        }
    }

    fun attack(user: User, x: Char, y: Int): Boolean {
        val actualX = x - 'a'
        return try {
            board[actualX][y].attack(user)
        } catch(e: ArrayIndexOutOfBoundsException) {
            false
        }
    }

    class Location constructor(@Expose val x: Int, @Expose val y: Int) {

        @Expose
        var isShip = false
        @Expose
        var isAttacked = false
            private set
        @Expose
        var whoAttacked: User? = null
            private set

        fun attack(user: User): Boolean {
            isAttacked = true
            whoAttacked = user
            return isShip
        }

        fun getTextRepresentation(): String {
            return if (!isAttacked) "⬜" else if (isShip) ":x:" else "⬛"
        }

    }

}