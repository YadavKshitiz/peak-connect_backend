package com.peakconnect

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PeakConnectApplication

fun main(args: Array<String>) {
    runApplication<PeakConnectApplication>(*args)
}
