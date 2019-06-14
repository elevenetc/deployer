package com.elevenetc

import com.elevenetc.bodies.*
import com.elevenetc.projects.AppsManager
import com.google.gson.Gson
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
import io.ktor.http.parseUrlEncodedParameters
import io.ktor.request.receive
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.li
import kotlinx.html.ul

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


@Suppress("unused") // Referenced in application.conf
fun Application.module() {

    val logger = Logger("logs", "requests-logs.txt")
    val appsManager = AppsManager()

    install(ContentNegotiation) {
        gson {}
    }

    val gson = Gson()

    routing {

        post("/web-hooks/github") {
            val payload = call.receiveText().parseUrlEncodedParameters()["payload"]
            val tag = gson.fromJson(payload, GitHubTag::class.java)

            call.respond(HttpStatusCode.OK)

            if (tag.ref.startsWith("release-")) {
                val userName = tag.repository.full_name.split("/")[0]
                val appName = tag.repository.full_name.split("/")[1]
                appsManager.newVersion(userName, appName, tag.ref, tag.repository.clone_url)
            } else {
                logger.log("skip-tag", tag.ref)
            }
        }

        post("/apps/state") {

            val body = call.receive(SetState::class)
            val appId = body.appId

            if (appsManager.contains(appId)) {
                val success = appsManager.setAppState(appId, body.action)

                if (success) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.BadRequest, FailedAction(body.action, appsManager.getAppState(appId)))
                }

            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        post("/apps/sources/delete") {
            val body = call.receive(AppId::class)

            if (appsManager.contains(body.appId)) {
                appsManager.deleteSources(body.appId)
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
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
