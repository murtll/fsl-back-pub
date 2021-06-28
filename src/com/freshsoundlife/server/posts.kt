package com.freshsoundlife.server

import com.freshsoundlife.auth.Role
import com.freshsoundlife.entity.Post
import com.freshsoundlife.server.extensions.withRole
import com.jillesvangurp.eskotlinwrapper.dsl.bool
import com.jillesvangurp.eskotlinwrapper.dsl.match
import com.jillesvangurp.eskotlinwrapper.dsl.terms
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import org.elasticsearch.action.search.configure
import org.elasticsearch.action.search.source

@KtorExperimentalAPI
fun Routing.posts() = route("/post") {
    authenticate {
        withRole(Role.ADMIN) {
            post("/add") {
                val post = call.receive<Post>()

                postRepo.index(post.id, post)

                call.respond(HttpStatusCode.OK)
            }
        }

        delete("/{id}") {
            val id = call.parameters["id"] ?: throw BadRequestException("No value for parameter 'id' passed.")

            postRepo.delete(id);

            call.respond(HttpStatusCode.OK)
        }
    }
    get("/{id}") {
        val id = call.parameters["id"] ?: throw BadRequestException("No value for parameter 'id' passed.")

        if (id == "all") {
            val list = postRepo.search {
                source(
                    """
                        {
                            "size": 20,
                            "query": {
                                "match_all": {}
                            },
                            "sort": [
                                {
                                    "date": {
                                        "order": "desc"
                                    }
                                }
                            ]
                        }
                    """.trimIndent()
                )
            }.mappedHits.toList()
            call.respond(list)
        } else {
            val post = postRepo.get(id) ?: throw BadRequestException("No such post exists")
            call.respond(post)
        }
    }
}
