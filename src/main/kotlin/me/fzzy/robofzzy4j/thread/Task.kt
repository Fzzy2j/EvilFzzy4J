package me.fzzy.robofzzy4j.thread

import me.fzzy.robofzzy4j.running
import java.util.*

class IndividualTask constructor(var toRun: () -> Unit, var intervalSeconds: Int, var repeat: Boolean)

class Task : Thread() {

    private var tasks: HashMap<IndividualTask, Long> = hashMapOf()

    fun registerTask(individualTask: IndividualTask): IndividualTask {
        tasks[individualTask] = System.currentTimeMillis()
        return individualTask
    }

    override fun run() {
        while (running) {
            Thread.sleep(1000)

            for ((task, time) in tasks) {
                if ((System.currentTimeMillis() - time) / 1000 > task.intervalSeconds) {
                    try {
                        task.toRun.invoke()
                    } catch (e: Exception) {
                        println("Failed to run task!")
                        e.printStackTrace()
                    }
                    if (task.repeat)
                        tasks[task] = System.currentTimeMillis()
                    else
                        tasks.remove(task)
                    break
                }
            }
        }
    }

}