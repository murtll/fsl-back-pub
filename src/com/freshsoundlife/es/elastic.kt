package com.freshsoundlife.es

import com.freshsoundlife.config.elasticHost
import com.freshsoundlife.config.elasticPassword
import com.freshsoundlife.config.elasticPort
import com.freshsoundlife.config.elasticUser
import com.freshsoundlife.dependency.kodein
import com.typesafe.config.Config
import org.elasticsearch.client.create
import org.kodein.di.generic.instance

private val config: Config by kodein.instance()

val esClient = create(
    host = config.elasticHost,
    port = config.elasticPort,
    user = config.elasticUser,
    password = config.elasticPassword,
    https = true)