package com.freshsoundlife.dependency

import com.freshsoundlife.auth.SimpleJWT
import com.freshsoundlife.entity.Post
import com.freshsoundlife.entity.User
import com.freshsoundlife.es.esClient
import com.icerockdev.service.email.MailerService
import com.icerockdev.service.email.SMTPConfig
import com.icerockdev.service.email.SMTPSecure
import com.jillesvangurp.eskotlinwrapper.IndexRepository
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.elasticsearch.client.indexRepository
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.singleton
import com.cloudinary.Cloudinary
import com.fasterxml.jackson.databind.ObjectMapper

val kodein = Kodein {
    bind<Config>() with singleton { ConfigFactory.load() }
    bind<IndexRepository<User>>() with singleton { esClient.indexRepository<User>("users") }
    bind<IndexRepository<Post>>() with singleton { esClient.indexRepository<Post>("posts") }
//    bind<IndexRepository<Map<String, Date>>>() with singleton { esClient.indexRepository<Map<String, Date>>("blacklist") }
    bind<SimpleJWT>() with singleton { SimpleJWT(ConfigFactory.load().getString("jwt.secret")) }
    bind<MailerService>() with singleton {
        MailerService(
            CoroutineScope(Dispatchers.IO + SupervisorJob()),
            SMTPConfig(
                // TODO: 2/15/21 FIX DAT SHIT AFTER RELEASE KURWA
                host = "smtp.mail.ru",
//                host = "in-v3.mailjet.com",
                port = 587,
//                port = 25,
                smtpSecure = SMTPSecure.TLS,
                smtpAuth = true,
                username = "freshsoundlife@mail.ru",
//                username = "c7f3c202ea7d4b50072edd40843cfd6c",
                password = "qbbhtxlkmhpnrlfd"
//                password = "36fef5a9c450fdcd2b3ac34c16a95965"
            )
        )
    }
    bind<Cloudinary>() with singleton {
        Cloudinary(mapOf("cloud_name" to "dmq4aevks"))
    }
    bind<ObjectMapper>() with singleton {
        ObjectMapper()
    }
}