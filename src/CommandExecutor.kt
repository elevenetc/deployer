package com.elevenetc

import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.streams.toList


class CommandExecutor {

    data class Result(val exitValue: Int, val out: List<String>)

    fun run(
        cmd: String,
        workingDir: String = "",
        envVars: Map<String, String> = emptyMap()
    ): Result {

        var process = newExec(envVars, cmd, workingDir)
        val stdInput = BufferedReader(InputStreamReader(process.inputStream))
        val stdError = BufferedReader(InputStreamReader(process.errorStream))
        val result = mutableListOf<String>()

        val buffer = CharArray(4096)

        var count = stdInput.read(buffer)
        while (count != -1) {
            val data = String(buffer, 0, count)
            println(data)
            count = stdInput.read(buffer)
        }

        count = stdError.read(buffer)
        while (count != -1) {
            val data = String(buffer, 0, count)
            println(data)
            count = stdError.read(buffer)
        }

        process.waitFor()

        if (process.exitValue() != 0) {
            println("Result exit message: \n$result")
            println("Error exit message: \n" + readLines(process.errorStream))
            throw RuntimeException("Command $cmd finished with ${process.exitValue()}")
        }

        return Result(process.exitValue(), result)
    }

    private fun readLines(stream: InputStream): List<String> {
        val inputStream = BufferedReader(InputStreamReader(stream))
        val result = inputStream.lines().toList()
        return result
    }

    private fun newExec(
        envVars: Map<String, String>,
        cmd: String,
        workingDir: String
    ): Process {
        val pBuilder = ProcessBuilder()
        pBuilder.command(cmd.split(" "))
        pBuilder.environment().putAll(envVars)
        pBuilder.directory(if (workingDir.isEmpty()) null else File(workingDir))
        return pBuilder.start()
    }

    private fun oldExec(
        envVars: Map<String, String>,
        cmd: String,
        workingDir: String
    ): Process {
        val e = mutableMapOf<String, String>()
        e.putAll(System.getenv())
        e.putAll(envVars)
        val envVarsString = e.map { i -> "${i.key}=${i.value}" }.toList().toTypedArray()

        var process = Runtime.getRuntime().exec(
            cmd,
            envVarsString,
            if (workingDir.isEmpty()) null else File(workingDir)
        )
        return process
    }
}