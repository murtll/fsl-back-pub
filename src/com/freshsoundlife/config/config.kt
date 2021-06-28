package com.freshsoundlife.config

import com.typesafe.config.Config

val Config.elasticHost: String
    get() = this.getString("elastic.host")

val Config.elasticPort: Int
    get() = this.getString("elastic.port").toInt()

val Config.elasticUser: String
    get() = this.getString("elastic.user")

val Config.elasticPassword: String
    get() = this.getString("elastic.password")
