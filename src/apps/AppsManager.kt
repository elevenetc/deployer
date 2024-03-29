package com.elevenetc.projects

import com.elevenetc.FileSystem
import com.elevenetc.apps.AppLoader
import com.elevenetc.bodies.EnvVar
import java.nio.file.Files
import java.nio.file.Paths

class AppsManager {

    private val fileSystem = FileSystem()
    private val appLoaded = AppLoader()
    private val apps = mutableMapOf<String, App>()
    private val appsDir = "apps"

    init {
        fileSystem.createDirectory(appsDir)

        Thread {
            initApps()
        }.start()
    }

    fun contains(appId: String): Boolean {
        return apps.keys.contains(appId)
    }

    fun getAppState(appId: String): String {
        return apps[appId]!!.data.state
    }

    fun setAppState(appId: String, action: String): Boolean {

        var success = false
        val app = apps[appId]!!

        if (action == Action.CLONE) {
            app.clone()
            success = true
        } else if (action == Action.START) {
            app.run()
            success = true
        } else if (action == Action.BUILD) {
            app.build()
            success = true
        } else if (action == Action.STOP) {
            app.stop()
            success = true
        }

        return success
    }

    fun setCommands(appId: String, commands: App.AppData.Commands) {
        apps.filter {
            it.key == appId
        }.forEach { _, app ->
            app.data.commands.buildCommands.clear()
            app.data.commands.startCommands.clear()
            app.data.commands.stopCommands.clear()
            app.data.commands.onFinishCommands.clear()

            app.data.commands.buildCommands.addAll(commands.buildCommands)
            app.data.commands.startCommands.addAll(commands.startCommands)
            app.data.commands.stopCommands.addAll(commands.stopCommands)
            app.data.commands.onFinishCommands.addAll(commands.onFinishCommands)

            app.persistData()
        }
    }

    fun setEnvVars(appId: String, envVars: List<EnvVar>) {
        apps.filter {
            it.key == appId
        }.forEach { _, app ->
            app.data.envVars.clear()
            app.data.envVars.addAll(envVars)
            app.persistEnvVars()
        }
    }

    fun deleteSources(appId: String) {
        val app = apps[appId]!!
        fileSystem.delete(app.data.appSourcesDir)
        app.updateState(App.State.NEW)
    }

    fun apps(): Map<String, App> {
        return apps.toMap()
    }

    private fun initApps() {
        Files.walk(Paths.get(appsDir))
            .filter { it.fileName.toString() == App.DATA_JSON_NAME }
            .forEach {
                val appStatePath = it.toString()
                val app = appLoaded.load(appStatePath)
                apps[app.data.id] = app
            }

        apps.forEach { _, app ->

            if (!app.data.isCloned) {
                app.cloneBuildRun()
            } else if (app.data.isBuilt) {
                app.buildAndRun()
            } else {
                app.run()
            }
        }
    }

    fun newVersion(userName: String, appName: String, tag: String, cloneUrl: String) {

        val appId = "$userName/$appName"
        val appDir = "$appsDir/$appId"
        val appDirSources = "$appDir/sources"

        if (apps.containsKey(appId)) {
            println("update version...")
        } else {
            val state = App.AppData(appId, appName, userName, tag, appDir, appDirSources, cloneUrl)

            val app = App(state)
            apps[app.data.id] = app

            app.updateState(App.State.NEW)
            app.cloneBuildRun()
        }
    }

    class Action {
        companion object {
            const val BUILD = "build"
            const val START = "start"
            const val STOP = "stop"
            const val CLONE = "clone"
        }
    }
}