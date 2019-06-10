package com.elevenetc

import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.streams.toList


class CommandExecutor {

    fun run(
        cmd: String,
        workingDir: String = "",
        envVars: Map<String, String> = emptyMap()
    ): List<String> {


        var process = newExec(envVars, cmd, workingDir)
        //var process = oldExec(envVars, cmd, workingDir)

        val result = readLines(process.inputStream)

        println("exit: " + process.exitValue())

        if (process.exitValue() != 0) {
            println("Result exit message: \n$result")
            println("Error exit message: \n" + readLines(process.errorStream))
            throw RuntimeException("Command $cmd finished with ${process.exitValue()}")
        }

        return result
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