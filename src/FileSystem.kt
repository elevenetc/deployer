package com.elevenetc

import com.google.gson.Gson
import java.io.File
import java.io.FileReader
import java.io.PrintWriter

class FileSystem {

    val gson = Gson()

    fun exists(dir: String): Boolean {
        return File(dir).exists()
    }

    fun delete(dir: String) {
        File(dir).deleteRecursively()
    }

    fun <T> getFile(path: String, clazz: Class<T>): T? {
        return if (exists(path)) {
            val reader = FileReader(File(path))
            val str = reader.readText()
            reader.close()
            gson.fromJson(str, clazz)
        } else {
            null
        }
    }

    fun writeFile(data: String, path: String) {
        val writer = PrintWriter(path, "UTF-8")
        writer.println(data)
        writer.close()
    }

    fun writeFile(obj: Any, path: String) {
        writeFile(gson.toJson(obj), path)
    }

    fun createDirectory(dir: String) {

        val dirFile = File(dir)

        if (!dirFile.exists()) {
            dirFile.mkdirs()
        }
    }
}