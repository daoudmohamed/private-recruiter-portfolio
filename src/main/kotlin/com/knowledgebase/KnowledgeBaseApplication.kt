package com.knowledgebase

import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class KnowledgeBaseApplication

fun main(args: Array<String>) {
    runApplication<KnowledgeBaseApplication>(*args)
}
