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

        if (!data.commands.buildCommands.isEmpty()) {
            var result = 0
            data.commands.buildCommands.forEach { cmd ->
                result += runCommand(cmd)
            }
            data.isBuilt = result == 0
        }

        updateState(State.BUILT)
    }

    private fun cloneTask() {
        updateState(State.CLONING)
        runCommand(
            "git clone --branch ${data.tag} ${data.cloneUrl} --depth 1 ${data.appSourcesDir}",
            ""
        )
        data.isCloned = true
        updateState(State.CLONED)
    }

    fun buildAndRun() {
        commandsPool.submit {
            buildTask()
            runTask()
        }
    }

    fun cloneBuildRun() {
        commandsPool.submit {
            cloneTask()
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
    ): Int {

        var exitValue = 0

        try {
            logger.log("cmd", workingDir + cmd)
            println("executing: $workingDir$cmd")
            val result = CommandExecutor().run(cmd, workingDir)
            println("result: $result")
            exitValue = result.exitValue
        } catch (t: Throwable) {
            logger.log("cmd: $cmd", t)
            exitValue = 666
        }

        return exitValue
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

        var isBuilt = false
        var isCloned = false

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