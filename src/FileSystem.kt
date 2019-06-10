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

    fun writeFile(obj: Any, path: String) {
        val raw = gson.toJson(obj)
        val writer = PrintWriter(path, "UTF-8")
        writer.println(raw)
        writer.close()
    }

    fun createDirectory(dir: String) {

        val dirFile = File(dir)

        if (!dirFile.exists()) {
            dirFile.mkdirs()
        }
    }
}