package com.peakconnect.service

import com.peakconnect.dto.BookingConfirmDto
import com.peakconnect.entity.*
import com.peakconnect.repository.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class BookingConcurrencyTest {

    @Autowired
    lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    lateinit var bookingService: BookingService

    @Autowired
    lateinit var slotRepository: SlotRepository

    @Autowired
    lateinit var activityRepository: ActivityRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var guideRepository: GuideRepository

    @Test
    fun `test concurrent booking limits occupancy to capacity`() {
        // Setup data in a transaction so it gets committed for other threads to see
        var guideId = ""
        var slotId = ""
        val trekkerEmails = mutableListOf<String>()

        transactionTemplate.execute {
            val uniqueSuffix = java.util.UUID.randomUUID().toString().substring(0, 8)
            val guideUser = userRepository.save(User(name = "Guide Test", email = "guide_test_$uniqueSuffix@example.com", password = "pwd", role = Role.GUIDE))
            val guide = guideRepository.save(Guide(user = guideUser, experienceLevel = ExperienceLevel.EXPERT, isVerified = true))
            guideId = guide.id.toString()
            
            val activity = activityRepository.save(Activity(
                title = "Concurrency Trek",
                description = "Test",
                location = "Test Loc",
                difficultyLevel = DifficultyLevel.MODERATE,
                basePrice = BigDecimal("100")
            ))
            
            // Capacity = 1
            val slot = slotRepository.save(Slot(
                activity = activity,
                date = LocalDateTime.now().plusDays(5),
                capacity = 1,
                season = Season.SHOULDER
            ))
            slotId = slot.id.toString()

            // Create 3 trekkers
            for (i in 1..3) {
                val trekker = userRepository.save(User(name = "Trekker $i", email = "trekker${i}_$uniqueSuffix@example.com", password = "pwd", role = Role.TREKKER))
                trekkerEmails.add(trekker.email)
            }
        }

        val numThreads = 3
        val executor = Executors.newFixedThreadPool(numThreads)
        val latch = CountDownLatch(1)
        val doneLatch = CountDownLatch(numThreads)
        
        val successfulBookings = AtomicInteger(0)
        val failedBookings = AtomicInteger(0)

        for (i in 0 until numThreads) {
            val trekkerEmail = trekkerEmails[i]
            executor.submit {
                try {
                    latch.await() // Wait until all threads are ready
                    val dto = BookingConfirmDto(java.util.UUID.fromString(slotId), java.util.UUID.fromString(guideId))
                    bookingService.confirmBooking(dto, trekkerEmail)
                    successfulBookings.incrementAndGet()
                    println("Thread ${Thread.currentThread().name}: Booking SUCCEEDED for $trekkerEmail")
                } catch (e: Exception) {
                    failedBookings.incrementAndGet()
                    println("Thread ${Thread.currentThread().name}: Booking FAILED for $trekkerEmail - ${e.message}")
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        println("Starting concurrent booking requests...")
        latch.countDown() // Release all threads at the same time
        doneLatch.await() // Wait for all threads to finish
        executor.shutdown()

        println("Successful bookings: ${successfulBookings.get()}")
        println("Failed bookings: ${failedBookings.get()}")

        // Assertions
        val updatedSlot = slotRepository.findById(java.util.UUID.fromString(slotId)).get()
        assertEquals(1, successfulBookings.get(), "Exactly one booking should succeed")
        assertEquals(2, failedBookings.get(), "Two bookings should fail")
        assertEquals(1, updatedSlot.currentOccupancy, "Slot occupancy should be exactly 1")
    }
}
