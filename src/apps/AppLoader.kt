package com.elevenetc.apps

import com.elevenetc.FileSystem
import com.elevenetc.projects.App

class AppLoader {

    private val fileSystem = FileSystem()

    fun load(appStatePath: String): App {
        val appState = fileSystem.getFile(appStatePath, App.AppData::class.java)!!
        return App(appState)
    }
}