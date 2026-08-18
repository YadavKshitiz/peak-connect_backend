package com.peakconnect

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class PeakConnectApplication

fun main(args: Array<String>) {
    runApplication<PeakConnectApplication>(*args)
}
