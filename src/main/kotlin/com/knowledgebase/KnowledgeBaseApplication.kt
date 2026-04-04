package com.knowledgebase

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KnowledgeBaseApplication

fun main(args: Array<String>) {
    runApplication<KnowledgeBaseApplication>(*args)
}