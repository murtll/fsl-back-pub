package com.freshsoundlife.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.Payload
import com.freshsoundlife.auth.Role
import java.util.*

class SimpleJWT(secret: String) {
    private val algorithm = Algorithm.HMAC512(secret)
    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer("FSL")
        .build()

    fun sign(userId: String, email: String, username: String, expDate: Date, role: Role): String = JWT
        .create()
        .withExpiresAt(expDate)
        .withIssuer("FSL")
        .withClaim("id", userId)
        .withClaim("email", email)
        .withClaim("username", username)
        .withClaim("role", role.name)
        .sign(algorithm)

    fun decode(token: String): Payload = JWT
        .decode(token)
}