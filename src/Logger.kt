package com.elevenetc

import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*


class Logger(
    private val directory: String,
    private val logFileName: String
) {

    private val fileSystem = FileSystem()
    private val gson = Gson()
    private val filePath = "$directory/$logFileName"

    init {
        checkFile()
    }

    fun log(tag: String, t: Throwable) {
        val writer = StringWriter()
        t.printStackTrace(PrintWriter(writer))
        log("error-$tag", writer.toString())
    }

    fun log(tag: String, message: String) {

        println("$tag: $message")

        PrintWriter(FileOutputStream(File(filePath), true)).apply {
            println(gson.toJson(Message(tag, message)))
            close()
        }
    }

    private fun checkFile() {

        fileSystem.createDirectory(directory)

        val logs = File(filePath)
        if (!logs.exists()) {
            val writer = PrintWriter(filePath, "UTF-8")
            writer.println(gson.toJson(Init()))
            writer.close()
        }
    }

    data class Init(val initDate: Long = Date().time)
    data class Message(
        val tag: String,
        val message: String,
        val date: Long = Date().time
    )
}