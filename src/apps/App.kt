package com.elevenetc.projects

import com.elevenetc.CommandExecutor
import com.elevenetc.FileSystem
import com.elevenetc.Logger
import com.elevenetc.bodies.EnvVar

class App(val data: AppData) {

    private val logger = Logger(data.appDir, "app-logs.txt")
    private val fileSystem = FileSystem()

    init {

    }

    fun clone() {
        updateState(State.CLONING)
        runCommand("git clone --branch ${data.tag} ${data.cloneUrl} --depth 1")
        updateState(State.CLONED)
    }

    fun build() {
        updateState(State.BUILDING)
        data.commands.buildCommands.forEach { cmd -> runCommand(cmd) }
        updateState(State.BUILT)
    }

    fun updateState(s: String) {
        logger.log("state", s)

        if (s == State.NEW) {
            fileSystem.createDirectory(data.appDir)
            fileSystem.createDirectory(data.appSourcesDir)
        }

        data.update(s)
        persist()
    }

    fun persist() {
        fileSystem.writeFile(data, data.stateFilePath)
    }

    private fun runCommand(cmd: String, envVars: Map<String, String> = emptyMap()) {
        logger.log("cmd", cmd)
        println("executing: $cmd")
        println(
            "result: " +
                    CommandExecutor().run(
                        cmd,
                        System.getProperty("user.dir") + "/" + data.appSourcesDir + "/",
                        envVars
                    )
        )
    }

    fun run() {
        updateState(State.RUNNING)
        data.commands.startCommands.forEach { cmd -> runCommand(cmd) }
        updateState(State.FINISHING)
        data.commands.onFinishCommands.forEach { cmd -> runCommand(cmd) }
        updateState(State.FINISHED)
    }

    fun stop() {
        updateState(State.STOPPING)
        data.commands.stopCommands.forEach { cmd -> runCommand(cmd) }
        updateState(State.FINISHING)
        data.commands.onFinishCommands.forEach { cmd -> runCommand(cmd) }
        updateState(State.FINISHED)
    }

    class AppData(
        val id: String,
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