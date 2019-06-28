package com.elevenetc.projects

import com.elevenetc.CommandExecutor
import com.elevenetc.FileSystem
import com.elevenetc.Logger
import com.elevenetc.bodies.EnvVar
import java.util.concurrent.Executors

class App(val data: AppData) {

    private val logger = Logger(data.appDir, "app-logs.txt")
    private val fileSystem = FileSystem()

    private val commandsPool = Executors.newFixedThreadPool(10) { r ->
        Thread(r).apply {
            name = "commands-thread"
        }
    }

    private fun stopTask() {
        updateState(State.STOPPING)
        data.commands.stopCommands.forEach { cmd -> runCommand(cmd) }
        updateState(State.FINISHING)
        data.commands.onFinishCommands.forEach { cmd -> runCommand(cmd) }
        updateState(State.FINISHED)
    }

    private fun runTask() {
        updateState(State.RUNNING)
        data.commands.startCommands.forEach { cmd -> runCommand(cmd) }
        updateState(State.FINISHING)
        data.commands.onFinishCommands.forEach { cmd -> runCommand(cmd) }
        updateState(State.FINISHED)
    }

    private fun buildTask() {
        updateState(State.BUILDING)
        data.commands.buildCommands.forEach { cmd -> runCommand(cmd) }
        updateState(State.BUILT)
    }

    private fun cloneTask() {
        updateState(State.CLONING)
        runCommand(
            "git clone --branch ${data.tag} ${data.cloneUrl} --depth 1 ${data.appSourcesDir}",
            ""
        )
        updateState(State.CLONED)
    }

    fun buildAndRun(){
        commandsPool.submit {
            buildTask()
            runTask()
        }
    }

    fun clone() {
        commandsPool.submit { cloneTask() }
    }

    fun build() {
        commandsPool.submit { buildTask() }
    }

    fun run() {
        commandsPool.submit { runTask() }
    }

    fun stop() {
        commandsPool.submit { stopTask() }
    }

    fun persistEnvVars() {
        fileSystem.writeFile(dockerEnvVars(), data.appSourcesDir + "/.env")
    }

    fun persistData() {
        fileSystem.writeFile(data, data.stateFilePath)
    }

    private fun runCommand(
        cmd: String,
        workingDir: String = System.getProperty("user.dir") + "/" + data.appSourcesDir + "/"
    ) {

        commandsPool.submit {

            try {
                logger.log("cmd", workingDir + cmd)
                println("executing: $workingDir$cmd")
                println(
                    "result: " +
                            CommandExecutor().run(
                                cmd,
                                workingDir
                            )
                )
            } catch (t: Throwable) {
                logger.log("cmd: $cmd", t)
            }


        }

    }

    fun updateState(s: String) {

        logger.log("state", s)

        if (s == State.NEW) {
            fileSystem.createDirectory(data.appDir)
            fileSystem.createDirectory(data.appSourcesDir)
        }

        data.update(s)
        persistData()
    }

    fun dockerEnvVars(): String {
        val sb = StringBuilder()
        data.envVars.forEach {
            sb.append(it.key + "=" + it.value + "\n")
        }
        return sb.toString()
    }

    class AppData(
        val id: String,
        val appName: String,
        val userName: String,
        val tag: String,
        val appDir: String,
        val appSourcesDir: String,
        val cloneUrl: String,
        val commands: Commands = Commands(),
        val envVars: MutableList<EnvVar> = mutableListOf()
    ) {

        var state: String = State.NEW
        val stateFilePath = "$appDir/$DATA_JSON_NAME"

        fun update(state: String) {
            this.state = state
        }

        data class Commands(
            val buildCommands: MutableList<String> = mutableListOf(),
            val startCommands: MutableList<String> = mutableListOf(),
            val stopCommands: MutableList<String> = mutableListOf(),
            val onFinishCommands: MutableList<String> = mutableListOf()
        )
    }

    companion object {
        const val DATA_JSON_NAME = "app-data.json"
    }

    class State {

        companion object {
            const val NEW = "new"
            const val CLONING = "cloning"
            const val CLONED = "cloned"
            const val BUILDING = "building"
            const val BUILT = "built"
            const val RUNNING = "running"
            const val STOPPING = "stopping"
            const val FINISHING = "finishing"
            const val FINISHED = "finished"
        }
    }

}