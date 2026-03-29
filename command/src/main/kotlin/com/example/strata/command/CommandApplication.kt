package com.example.strata.command

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CommandApplication

fun main(args: Array<String>) {
    runApplication<CommandApplication>(*args)
}
