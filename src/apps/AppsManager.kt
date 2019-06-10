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
        initApps()
    }

    fun contains(appId: String): Boolean {
        return apps.keys.contains(appId)
    }

    fun setEnvVars(appId: String, envVars: List<EnvVar>) {
        apps.filter {
            it.key == appId
        }.forEach { _, app ->
            app.data.envVars.clear()
            app.data.envVars.addAll(envVars)
            app.persist()
        }
    }

    fun delete(name: String) {
        if (apps.containsKey(name)) {

        }
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
            if (app.data.state == App.State.CLONED.toString() ||
                app.data.state == App.State.BUILDING.toString()
            ) {
                app.build()
                //app.run()
            } else if (
                app.data.state == App.State.BUILT.toString() ||
                app.data.state == App.State.FINISHED.toString() ||
                app.data.state == App.State.RUNNING.toString()
            ) {
                //app.run()
            }
        }
    }

    fun newTag(appId: String, tag: String, cloneUrl: String) {

        val appDir = "$appsDir/$appId"
        val appDirSources = "$appDir/sources"

        if (!fileSystem.exists(appDir)) {
            val state = App.AppData(appId, tag, appDir, appDirSources, cloneUrl)

            fileSystem.createDirectory(appDir)
            fileSystem.createDirectory(appDirSources)

            val app = App(state)
            apps[app.data.id] = app

            app.updateState(App.State.NEW)
            app.clone()
            app.build()
            app.run()
        }
    }
}