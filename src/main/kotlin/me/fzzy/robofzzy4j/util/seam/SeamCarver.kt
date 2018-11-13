package me.fzzy.robofzzy4j.util.seam

import me.fzzy.robofzzy4j.RoboFzzy
import java.awt.Color
import java.util.Stack
import java.util.concurrent.Future

class SeamCarver {

    private val shortestPathFinder = ShortestPathFinder()
    private val energyCalculator = EnergyCalculator()

    var verticalSeams = Stack<IntArray>()
        private set
    var horizontalSeams = Stack<IntArray>()
        private set

    fun resize(initialPicture: Picture, removeColumns: Int, removeRows: Int): Picture {
        verticalSeams = Stack()
        horizontalSeams = Stack()
        var picture = removeColumns(initialPicture, removeColumns, true)
        picture = removeRows(picture, removeRows)
        val bufferedPicture = BufferedImagePicture(picture.width, picture.height)
        for (x in 0 until picture.width) {
            for (y in 0 until picture.height) {
                bufferedPicture.set(x, y, picture.get(x, y))
            }
        }
        return bufferedPicture
    }

    private fun removeRows(picture: Picture, removeRows: Int): Picture {
        var transpose: Picture = TransposePicture(picture)
        transpose = removeColumns(transpose, removeRows, false)
        return TransposePicture(transpose)
    }

    private fun removeColumns(picture: Picture, removeColumns: Int, isVertical: Boolean): Picture {
        var picture = picture
        for (i in 0 until removeColumns) {

            val energy = energyCalculator.computeEnergyMulti(picture)
            val seam = findSeam(energy)
            picture = removeSeam(picture, seam)

            if (isVertical) {
                verticalSeams.push(seam)
            } else {
                horizontalSeams.push(seam)
            }
        }
        return picture
    }

    private fun findSeam(energy: Array<DoubleArray>): IntArray {
        return shortestPathFinder.findShortestPath(energy)
    }

    private fun removeSeam(picture: Picture, seam: IntArray): Picture {
        val newPicture = ArrayPicture(picture.width - 1, picture.height)
        val futureList = arrayListOf<Future<*>>()
        for (y in 0 until picture.height) {
            futureList.add(RoboFzzy.executor.submit {
                var i = 0
                for (x in 0 until picture.width) {
                    if (x != seam[y]) {
                        newPicture.set(i++, y, picture.get(x, y))
                    }
                }
            })
        }
        for (future in futureList)
            future.get()
        return newPicture
    }

    fun addVerticalSeam(picture: Picture, seam: IntArray, seamColor: Color): Picture {
        val newPicture = ArrayPicture(picture.width + 1, picture.height)
        for (y in 0 until newPicture.height) {
            for (x in 0 until newPicture.width) {
                if (x == seam[y]) {
                    newPicture.set(seam[y], y, seamColor)
                } else if (x < seam[y]) {
                    newPicture.set(x, y, picture.get(x, y))
                } else {
                    newPicture.set(x, y, picture.get(x - 1, y))
                }
            }
        }
        return newPicture
    }

    fun addHorizontalSeam(picture: Picture, seam: IntArray, seamColor: Color): Picture {
        val newPicture = ArrayPicture(picture.width, picture.height + 1)
        for (y in 0 until newPicture.width) {
            for (x in 0 until newPicture.height) {
                if (x == seam[y]) {
                    newPicture.set(y, seam[y], seamColor)
                } else if (x < seam[y]) {
                    newPicture.set(y, x, picture.get(y, x))
                } else {
                    newPicture.set(y, x, picture.get(y, x - 1))
                }
            }
        }
        return newPicture
    }

}