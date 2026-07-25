package com.peakconnect

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration

// Disable DB autoconfig so the context test can pass without environment variables
@SpringBootTest(classes = [PeakConnectApplication::class])
@EnableAutoConfiguration(exclude = [DataSourceAutoConfiguration::class, HibernateJpaAutoConfiguration::class])
class PeakConnectApplicationTests {

    @Test
    fun contextLoads() {
    }

}
