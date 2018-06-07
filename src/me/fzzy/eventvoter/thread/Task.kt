package me.fzzy.eventvoter.thread

import me.fzzy.eventvoter.running
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
                if (System.currentTimeMillis() - time > task.intervalSeconds) {
                    task.toRun.invoke()
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