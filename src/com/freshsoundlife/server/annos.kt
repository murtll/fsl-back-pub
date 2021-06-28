package com.freshsoundlife.server

import com.fasterxml.jackson.databind.ObjectMapper
import com.freshsoundlife.auth.Role
import com.freshsoundlife.entity.Announcement
import com.freshsoundlife.entity.UserModel
import com.freshsoundlife.exception.UnauthorizedException
import com.freshsoundlife.server.extensions.withRole
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import org.elasticsearch.action.search.source
import org.elasticsearch.common.xcontent.stringify
import org.json.JSONException
import org.json.JSONObject

@OptIn(KtorExperimentalAPI::class)
fun Routing.annos() = route("/anno") {
    authenticate {
        post("/add") {
            val id = call.authentication.principal<UserModel>()?.id ?: throw UnauthorizedException()
            val announcement = call.receive<Announcement>()

            userRepo.update(id) {
                it.apply {
                    announcements.add(0, announcement)
                }
            }
            call.respond(HttpStatusCode.OK)
        }

        delete("/all") {
            val id = call.authentication.principal<UserModel>()?.id ?: throw UnauthorizedException()

            userRepo.update(id) {
                it.apply {
                    announcements.clear()
                }
            }

            call.respond(HttpStatusCode.OK)
        }

        delete("/{index}") {
            val userId = call.authentication.principal<UserModel>()?.id ?: throw UnauthorizedException()
            val index =
                call.parameters["index"]?.toInt() ?: throw BadRequestException("No value for parameter 'index' passed.")

            userRepo.update(userId) {
                it.apply {
                    announcements.removeAt(index)
                }
            }

            call.respond(HttpStatusCode.OK)
        }

        withRole(Role.ADMIN) {
            delete("/all/{id}") {
                val id = call.parameters["id"] ?: throw BadRequestException("No value for parameter 'id' passed.")

                userRepo.update(id) {
                    it.apply {
                        announcements.clear()
                    }
                }

                call.respond(HttpStatusCode.OK)
            }

        }
    }
    get("/all") {
        val annos = getAllAnnos()
        call.respond(annos)
    }

    get("/search") {
        val cities = call.request.queryParameters["cities"]?.split(" ")
        val online = call.request.queryParameters["online"]?.toBoolean()
        val genres = call.request.queryParameters["genres"]?.split(" ")

        val reqQuery = call.request.queryParameters["query"]?.run {
            split(" ")
                .toMutableList()
                .apply {
                    removeIf { it.length <= 2 }
                }.joinToString(" ")
                .toLowerCase()
        }

        val annos = mutableListOf<Map<String, Any>>()

        if (reqQuery != null) {
            val s = userRepo.search {
                source(
                    """{
                        "size": 500,
                        "query": {
                            "nested": {
                                "path": "announcements",
                                "query": {
                                    "bool": {
                                        "should": [
                                            {
                                                "match": {
                                                    "announcements.title": {
                                                        "query": "$reqQuery",
                                                        "boost": 2.0,
                                                        "fuzziness": "AUTO"
                                                    }
                                                }
                                            },
                                            {
                                                "match": {
                                                    "announcements.text": {
                                                        "query": "$reqQuery",
                                                        "boost": 1.5,
                                                        "fuzziness": "AUTO"
                                                    }
                                                }
                                            }
                                        ]
                                    }
                                },
                                "inner_hits": {}
                            }
                        }
                    }""".trimIndent()
                )
            }.searchResponse.stringify(pretty = true)

            val res = JSONObject(s).getJSONObject("hits").getJSONArray("hits")

            res.forEach {
                it as JSONObject
                val inner = it.getJSONObject("inner_hits").getJSONObject("announcements").getJSONObject("hits")
                    .getJSONArray("hits")

                inner.forEach { hit ->
                    hit as JSONObject
                    val id = hit.getString("_id") ?: throw BadRequestException("Smth strange - anno has not user")
                    val source = hit.getJSONObject("_source")

                    val anno = objectMapper.readValue(source.toString(), Announcement::class.java)

                    annos.add(mapOf("user" to id, "anno" to anno))
                }
            }

        } else {
            annos.addAll(getAllAnnos())
        }

        cities?.let { annos.removeIf { (!cities.contains((it["anno"] as Announcement).tags["city"])) } }
        online?.let { annos.removeIf { (it["anno"] as Announcement).tags["online"] != online } }
        genres?.let {
            annos.removeIf { (((it["anno"] as Announcement).tags["genres"] as List<*>?)?.matches(genres as List<*>) ?: 0 == 0) }
            annos.sortBy { -1*(((it["anno"] as Announcement).tags["genres"] as List<*>?)?.matches(genres as List<*>) ?: 0) }
        }

        call.respond(HttpStatusCode.OK, annos)
    }
}

fun getAllAnnos(): List<Map<String, Any>> {
    val annos = mutableListOf<Map<String, Any>>()
    userRepo.search {
        source(
            """
            {
                "size": 500,
                "query": {
                    "match_all": {}
                }
            }
        """.trimIndent()
        )
    }.mappedHits.forEach { user ->
        annos.addAll(
            user.announcements.map { anno ->
                mapOf(
                    "user" to user.id,
                    "anno" to anno
                )
            }
        )
    }
    return annos
}

fun <T> List<T>.matches(other: List<T>): Int {
    var count = 0

    this.forEach { first ->
        other.forEach { second ->
            if (first == second)
                count++
        }
    }

    return count
}

fun String.containsOneOf(patterns: List<String>): Boolean {
    patterns.forEach {
        if (this.contains(it.toLowerCase())) {
            return true
        }
    }
    return false
}