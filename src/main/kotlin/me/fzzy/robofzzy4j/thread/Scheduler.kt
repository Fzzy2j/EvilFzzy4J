package me.fzzy.robofzzy4j.thread

import sx.blah.discord.util.RequestBuffer
import java.util.*


object Scheduler : Thread() {

    private var tasks: HashMap<Task, Long> = hashMapOf()

    private fun addTask(task: Task): Task {
        tasks[task] = System.currentTimeMillis()
        return task
    }

    class Builder constructor(var timeSeconds: Int) {
        private var repeat = false
        private var action: () -> Unit = {}

        fun doAction(action: () -> Unit): Builder {
            this.action = action
            return this
        }

        fun repeat(): Builder {
            repeat = true
            return this
        }

        fun execute(): Task {
            val task =  Task(action, timeSeconds, repeat)
            addTask(task)
            return task
        }
    }

    class Task constructor(var action: () -> Unit, var timeSeconds: Int, var repeat: Boolean) {
        private var stopped = false
        fun stop() {
            stopped = true
        }
        fun isStopped(): Boolean {
            return stopped
        }
    }

    override fun run() {
        while (true) {
            sleep(1000)

            for ((task, time) in tasks) {
                if ((System.currentTimeMillis() - time) / 1000 > task.timeSeconds) {
                    if (task.isStopped()) {
                        tasks.remove(task)
                        continue
                    }
                    try {
                        task.action.invoke()
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