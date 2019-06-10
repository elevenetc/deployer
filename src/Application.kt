package com.elevenetc

import com.elevenetc.bodies.SetCommands
import com.elevenetc.bodies.SetEnvVars
import com.elevenetc.projects.AppsManager
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.html.respondHtml
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.default
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.request.path
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.li
import kotlinx.html.ul

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

data class KeyValueDto(val key: String, val value: String)
data class DtoApp(val id: String, val name: String, val envVars: List<KeyValueDto>)

@Suppress("unused") // Referenced in application.conf
fun Application.module() {

    val logger = Logger("logs", "requests-logs.txt")
    val appsManager = AppsManager()

    install(ContentNegotiation) {
        gson {}
    }

    routing {

        post("/web-hooks/github") {
            logger.log("request", call.request.path())
            val tag = call.receive(GitHubTag::class)
            appsManager.newTag(tag.full_name, tag.ref, tag.repository.clone_url)

            call.respond(HttpStatusCode.OK)
        }

        post("/apps/commands") {
            val body = call.receive(SetCommands::class)

            if (appsManager.contains(body.appId)) {
                appsManager.setCommands(body.appId, body.commands)
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        post("/apps/env-vars") {
            val body = call.receive(SetEnvVars::class)

            if (appsManager.contains(body.appId)) {
                appsManager.setEnvVars(body.appId, body.envVars)
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        get("/apps") {
            call.respond(appsManager.apps().values.map {
                it.data
            })
        }

        get("/html-dsl") {
            call.respondHtml {
                body {
                    h1 { +"HTML" }
                    ul {
                        for (n in 1..10) {
                            li { +"$n" }
                        }
                    }
                }
            }
        }

        static("static") {
            resources("static")
            default("index.html")
        }
    }
}
