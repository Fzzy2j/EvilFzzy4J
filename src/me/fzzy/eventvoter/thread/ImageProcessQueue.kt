package me.fzzy.eventvoter.thread

import me.fzzy.eventvoter.running
import java.util.*

interface ImageProcessTask {
    val code: () -> Any?
    fun finished(obj: Any?)
    fun queueUpdated(position: Int)
}

class ImageProcessQueue : Thread() {

    private var queue: ArrayList<ImageProcessTask> = arrayListOf()

    fun addToQueue(run: ImageProcessTask) {
        queue.add(run)
        run.queueUpdated(queue.indexOf(run))
    }

    override fun run() {
        while (running) {
            Thread.sleep(1000)

            if (queue.size > 0) {
                val file = queue[0].code.invoke()
                queue[0].finished(file)
                queue.removeAt(0)
                for (task in queue) {
                    task.queueUpdated(queue.indexOf(task))
                }
            }
        }
    }

}