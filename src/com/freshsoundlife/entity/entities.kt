package com.freshsoundlife.entity

import com.freshsoundlife.auth.Role
import io.ktor.auth.Principal
import io.ktor.http.cio.websocket.*
import java.util.*

data class User(
    val username: String,
    val email: String,
    val password: String?,
    val id: String = email.hashCode().toString(),
    val status: String = "inactive",
    var role: Role = Role.USER,
    val token: String = "",
    val avatar: String = "https://res.cloudinary.com/dmq4aevks/image/upload/v1614080674/user_dp5zlq.png",
    val announcements: MutableList<Announcement> = mutableListOf(),
    val demos: MutableList<String> = mutableListOf(),
    val subscriptions: MutableList<String> = mutableListOf(),
    val subscribers: MutableList<String> = mutableListOf()
) {
    fun toUserModel() = UserModel(this)
}

data class UserModel(
    val username: String,
    val email: String,
    val id: String = email.hashCode().toString(),
    val role: Role = Role.USER,
    val announcements: MutableList<Announcement> = mutableListOf(),
    val avatar: String = "https://res.cloudinary.com/dmq4aevks/image/upload/v1614080674/user_dp5zlq.png",
    val demos: MutableList<String> = mutableListOf(),
    val status: String = "inactive",
    val subscriptions: MutableList<String> = mutableListOf(),
    val subscribers: MutableList<String> = mutableListOf()
) : Principal {
    constructor(user: User) : this(
        user.username,
        user.email,
        user.id,
        user.role,
        user.announcements,
        user.avatar,
        user.demos,
        user.status,
        user.subscriptions,
        user.subscribers,
    )
}

data class Announcement(val title: String = "", val text: String = "", val tags: Map<String, Any?> = mapOf())

data class Post(
    val date: Date = Date(),
    val title: String,
    val text: String,
    val photoLink: String,
    val id: String = text.hashCode().toString()
)