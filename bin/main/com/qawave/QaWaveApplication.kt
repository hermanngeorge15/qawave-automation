package com.qawave

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class QaWaveApplication

fun main(args: Array<String>) {
    runApplication<QaWaveApplication>(*args)
}
