package com.freshsoundlife.server

import com.cloudinary.Cloudinary
import com.freshsoundlife.auth.Role
import com.freshsoundlife.dependency.kodein
import com.freshsoundlife.entity.User
import com.freshsoundlife.entity.UserModel
import com.freshsoundlife.exception.BadCredentialsException
import com.freshsoundlife.exception.UserAlreadyExistsException
import com.freshsoundlife.exception.UserNotExistsException
import com.freshsoundlife.server.extensions.addTokens
import com.freshsoundlife.server.extensions.withRole
import com.jillesvangurp.eskotlinwrapper.dsl.TermQuery
import com.jillesvangurp.eskotlinwrapper.dsl.bool
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import org.elasticsearch.action.search.dsl
import org.kodein.di.generic.instance
import org.mindrot.jbcrypt.BCrypt
import java.io.File

private val cloudinary: Cloudinary by kodein.instance()

@OptIn(KtorExperimentalAPI::class)
fun Routing.users() = route("/user") {
//    adding user
    post("/add") {
        val user = call.receive<User>()

        if (user.role == Role.ADMIN) throw BadRequestException("Can`t create admin user with no admin rights")

        userRepo.search {
            dsl {
                resultSize = 1
                query = bool {
                    must(
                        TermQuery("email.keyword", user.email)
                    )
                }
            }
        }.mappedHits.toList().takeIf { it.isNotEmpty() }?.let {
            throw UserAlreadyExistsException()
        }

//        confirmation key
/*
        val key = user.email.hashCode().toString() +
                user.username.hashCode().toString() +
                user.password.hashCode().toString()

//        sending confirmation email

        try {
            mailService.compose().apply {
                fromEmail = "freshsoundlife@mail.ru"
                fromName = "Fresh SoundLife"
                subject = "Email confirmation"
                to = mutableMapOf(user.email to user.username)
                html =
                    """<h1>PLS CONFIRM EMAIL</h1>
                    <b>Click <a href=http://localhost:8080/user/confirm/$key>here</a> to confirm</b>
                """.trimMargin()
            }.send()
        } catch (e: Exception) {
            throw BadRequestException(e.message ?: "Can't send email")
        }
*/


//        temporary solution
        // TODO: 2/23/21 fix emails
        userRepo.index(
            user.id,
            user.copy(
                password = BCrypt.hashpw(user.password, BCrypt.gensalt()),
                status = "active"
            )
        )

        call.response.addTokens(
            id = user.id,
            email = user.email,
            username = user.username,
            role = user.role
        )


        call.respond(HttpStatusCode.OK)
    }

    authenticate {
        withRole(Role.ADMIN) {
            delete("/{id}") {
                val id = call.parameters["id"] ?: throw BadRequestException("No value for parameter 'id' passed.")

                try {
                    userRepo.delete(id)
                    call.respond(HttpStatusCode.OK)
                } catch (e: Exception) {
                    throw BadRequestException(e.message ?: "Can`t delete user with id '$id'")
                }
            }

            post("/add/admin") {
                val user = call.receive<User>().apply {
                    role = Role.ADMIN
                }

                userRepo.search {
                    dsl {
                        resultSize = 1
                        query = bool {
                            must(
                                TermQuery("email.keyword", user.email)
                            )
                        }
                    }
                }.mappedHits.toList().takeIf { it.isNotEmpty() }?.let {
                    throw UserAlreadyExistsException()
                }
/*

                val key = user.email.hashCode().toString() +
                        user.username.hashCode().toString() +
                        user.password.hashCode().toString()

//        sending confirmation email
                try {
                    mailService.compose().apply {
                        fromEmail = "freshsoundlife@mail.ru"
//            fromEmail = "esskeetiter@gmail.com"
                        fromName = "Fresh SoundLife"
                        subject = "Email confirmation"
                        to = mutableMapOf(user.email to user.username)
                        html =
                            """<h1>PLS CONFIRM EMAIL</h1>
                    <b>Click <a href=http://localhost:8080/user/confirm/$key>here</a> to confirm</b>
                """.trimMargin()
                    }.send()
                } catch (e: Exception) {
                    throw BadRequestException(e.message ?: "Can't send email")
                }
*/

                //        temporary solution
                // TODO: 2/23/21 fix emails
                userRepo.index(
                    user.id,
                    user.copy(
                        password = BCrypt.hashpw(user.password, BCrypt.gensalt()),
                        status = "active"
                    )
                )

                call.response.addTokens(
                    id = user.id,
                    email = user.email,
                    username = user.username,
                    role = user.role
                )

                call.respond(HttpStatusCode.OK)
            }
        }

        get("/subscribe/{id}") {
            val subscriber = call.authentication.principal<UserModel>() ?: throw BadCredentialsException()
            val subscription = call.parameters["id"] ?: throw BadRequestException("No value for parameter 'id' passed")

            userRepo.update(subscriber.id) {
                it.apply {
                    subscriptions.add(subscription)
                }
            }

            userRepo.update(subscription) {
                it.apply {
                    subscribers.add(subscriber.id)
                }
            }

        }

        post("/avatar/change") {
            println("INSIDE AVATAR CHANGE")
            val multipart = call.receiveMultipart()
            println("MULTIPART RECEIVED")
            multipart.forEachPart { part ->
                println("INSIDE FOREACH")
                // if part is a file (could be form item)
                if (part is PartData.FileItem) {
                    println("INSIDE IF")
                    // retrieve file name of upload
                    val name = part.originalFileName!!
                    val file = File(name)
                    println("GOT FILE $name")
                    // use InputStream from part to save file
                    part.streamProvider().use { stream ->
                        println("INSIDE STREAM")
                        // copy the stream to the file with buffering
                        file.outputStream().buffered().use {
                            // this is blocking
                            stream.copyTo(it)
                        }
                        println("FILE COPIED")
                    }

                    val res = cloudinary
                        .uploader()
                        .unsignedUpload(
                            file, "ml_default", mapOf("resource_type" to "image")
                        )

                    println("FILE UPLOADED")

                    val currentUser = call.authentication.principal<UserModel>() ?: throw BadCredentialsException()

                    userRepo.update(currentUser.id) {
                        it.copy(
                            avatar = res["secure_url"] as String
                        )
                    }
                    file.delete()
                }
                // make sure to dispose of the part after use to prevent leaks
                part.dispose()
            }

            call.respond(HttpStatusCode.OK)

        }
//
//        post("/avatar-change") {
//            val avatar = call.receive<Map<String, String>>()["avatar"]
//                ?: throw BadRequestException("No value for parameter 'avatar' passed.")
//
//            val currentUser = call.authentication.principal<UserModel>() ?: throw BadCredentialsException()
//
//            userRepo.update(currentUser.id) {
//                it.copy(avatar = avatar)
//            }
//
//            call.respond(HttpStatusCode.OK)
//        }

/*
        post("/demo-add") {
            val audio = call.receive<Map<String, String>>()["audio"]
                ?: throw BadRequestException("No value for parameter 'audio' passed.")

            val currentUser = call.authentication.principal<UserModel>() ?: throw BadCredentialsException()

            userRepo.update(currentUser.id) {
                it.apply {
                    demos.add(0, audio)
                }
            }

            call.respond(HttpStatusCode.OK)
        }
*/

//        was demo-add
        post("/demo/add") {
            println("INSIDE DEMO")
            try {
                val multipart =
//                    runBlocking {
                    call.receiveMultipart()
//                }
                println("MULTIPART RECEIVED")
                multipart.forEachPart { part ->
                    // if part is a file (could be form item)
                    if (part is PartData.FileItem) {
                        // retrieve file name of upload
                        val name = part.originalFileName!!
                        println("GOT FILE $name")
                        val file = File(name).also {
                            println("tf is happenin")
                            println("mb its right")
                        }

                        // use InputStream from part to save file
                        part.streamProvider().use { stream ->
                            println("but there...")
                            // copy the stream to the file with buffering
                            file.outputStream().buffered().use {
                                println("and there...")
                                // this is blocking
                                stream.copyTo(it)
                            }
                            println("smth is ok")
                        }

                        call.respond(HttpStatusCode.OK)

//                        enable this if heroku error client connection interrupted will not fix
//                        call.respond(HttpStatusCode.OK)

//                    val cloudinary =
                        val res = cloudinary
                            .uploader()
                            .unsignedUpload(
                                file, "ml_default", mapOf("resource_type" to "auto")
                            )

                        val currentUser = call.authentication.principal<UserModel>() ?: throw BadCredentialsException()

                        userRepo.update(currentUser.id) {
                            it.apply {
                                demos.add(0, res["secure_url"] as String)
                            }
                        }
                        file.delete()
                    }
                    // make sure to dispose of the part after use to prevent leaks
                    part.dispose()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.BadRequest)
            }

//            call.respond(HttpStatusCode.OK)
        }

        post("/demo/add-v2/{name}") {
            println("INSIDE DEMO")
            try {
                val dataInputStream = call.receiveStream()
                println("STREAM RECEIVED")

                println(call.request.headers["Content-Type"])

                val name = call.parameters["name"] ?: "tmp_demo.mp3"
//                val file = File("tmp_demo.mp3")
                println(name)
                val file = File(name)

                dataInputStream.copyTo(file.outputStream().buffered())

                val res = cloudinary
                    .uploader()
                    .unsignedUpload(
                        file, "ml_default", mapOf("resource_type" to "auto")
                    )

                val currentUser = call.authentication.principal<UserModel>() ?: throw BadCredentialsException()

                userRepo.update(currentUser.id) {
                    it.apply {
                        demos.add(0, res["secure_url"] as String)
                    }
                }
                file.delete()

                call.respond(HttpStatusCode.OK)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.BadRequest)
            }

//            call.respond(HttpStatusCode.OK)
        }

        delete("/demo/all") {
            val currentUser = call.authentication.principal<UserModel>() ?: throw BadCredentialsException()

            userRepo.update(currentUser.id) {
                it.apply {
                    demos.clear()
                }
            }

            call.respond(HttpStatusCode.OK)
        }

        delete("/demo/{index}") {
            val currentUser = call.authentication.principal<UserModel>() ?: throw BadCredentialsException()
            val index =
                call.parameters["index"]?.toInt() ?: throw BadRequestException("No value for parameter 'index' passed.")

            userRepo.update(currentUser.id) {
                it.apply {
                    demos.removeAt(index)
                }
            }

            call.respond(HttpStatusCode.OK)
        }

        get("/me") {
            val currentUser = call.authentication.principal<UserModel>() ?: throw BadCredentialsException()

            val user = userRepo.get(currentUser.id)

            println("Got user $user")

            if (user != null) call.respond(user.toUserModel())
            else throw UserNotExistsException()
        }

    }

//    to confirm email
    get("/confirm/{key}") {
        val key = call.parameters["key"] ?: throw BadRequestException("No value for parameter 'key' passed.")

        val user = userRepo.search {
            dsl {
                resultSize = 1
                query = bool {
                    must(
                        TermQuery("status.keyword", key)
                    )
                }
            }
        }.mappedHits.toList().takeIf { it.isNotEmpty() }?.get(0) ?: throw BadCredentialsException()

        userRepo.index(user.id, user.copy(status = "active"), create = false)

        call.response.addTokens(
            id = user.id,
            email = user.email,
            username = user.username,
            role = user.role
        )
        call.respond(HttpStatusCode.OK)
    }

//    getting info about person
    get("/{id}") {
        val id = call.parameters["id"] ?: throw BadRequestException("No value for parameter 'id' passed.")

        if (id == "all") {
            val list = userRepo.search {
                source()
            }.mappedHits.map {
                it.toUserModel()
            }
            call.respond(list)
        } else {
            val user = userRepo.get(id)
            if (user != null) call.respond(user.toUserModel())
            else throw UserNotExistsException()
        }

    }
}
