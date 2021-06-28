package com.freshsoundlife.exception

import io.ktor.features.*

class BadCredentialsException : RuntimeException("Invalid credentials")

class UserNotExistsException : BadRequestException("User not exists")

class UserAlreadyExistsException : BadRequestException("User with this email already exists")

class UnauthorizedException : BadRequestException("User not authorized")

class NoNeededRoleException : BadRequestException("User don't have needed role")

class TokenStillValidException : BadRequestException("User already has valid token")