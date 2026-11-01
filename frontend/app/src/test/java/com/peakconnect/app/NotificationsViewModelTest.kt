package com.peakconnect.app

import com.peakconnect.app.core.session.SessionManager
import com.peakconnect.app.core.session.SessionState
import com.peakconnect.app.data.local.dao.UserContentDao
import com.peakconnect.app.data.local.entity.NotificationEntity
import com.peakconnect.app.domain.model.Role
import com.peakconnect.app.feature.notifications.NotificationsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val userContentDao: UserContentDao = mockk(relaxed = true)
    private val sessionManager: SessionManager = mockk(relaxed = true)

    private val notificationsFlow = MutableStateFlow<List<NotificationEntity>>(emptyList())
    private val sessionStateFlow = MutableStateFlow<SessionState>(
        SessionState.LoggedIn(role = Role.TREKKER, userId = "u1", email = "test@example.com")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { userContentDao.getNotificationsFlow() } returns notificationsFlow
        every { sessionManager.sessionState } returns sessionStateFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `view model observes notifications and updates ui state`() = runTest(testDispatcher) {
        val list = listOf(
            NotificationEntity(
                id = "n1",
                type = "BOOKING_UPDATE",
                title = "Guide Accepted",
                message = "Pay deposit",
                bookingId = "b1",
                createdAt = 1000L,
                isRead = false
            ),
            NotificationEntity(
                id = "n2",
                type = "PAYMENT_REMINDER",
                title = "Payment Reminder",
                message = "About 5 minutes left",
                bookingId = "b1",
                createdAt = 2000L,
                isRead = true
            )
        )
        notificationsFlow.value = list

        val viewModel = NotificationsViewModel(userContentDao, sessionManager)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.notifications.size)
        assertEquals("n1", state.notifications[0].id)
        assertFalse(state.notifications[0].isRead)
        assertTrue(state.notifications[1].isRead)
        assertEquals(Role.TREKKER, state.userRole)
    }

    @Test
    fun `markAsRead calls userContentDao markNotificationAsRead`() = runTest(testDispatcher) {
        coEvery { userContentDao.markNotificationAsRead("n1") } returns 1
        val viewModel = NotificationsViewModel(userContentDao, sessionManager)

        viewModel.markAsRead("n1")
        advanceUntilIdle()

        coVerify { userContentDao.markNotificationAsRead("n1") }
    }

    @Test
    fun `markAllAsRead calls userContentDao markAllNotificationsAsRead`() = runTest(testDispatcher) {
        coEvery { userContentDao.markAllNotificationsAsRead() } returns 2
        val viewModel = NotificationsViewModel(userContentDao, sessionManager)

        viewModel.markAllAsRead()
        advanceUntilIdle()

        coVerify { userContentDao.markAllNotificationsAsRead() }
    }

    @Test
    fun `deleteNotification calls userContentDao deleteNotification`() = runTest(testDispatcher) {
        coEvery { userContentDao.deleteNotification("n1") } returns 1
        val viewModel = NotificationsViewModel(userContentDao, sessionManager)

        viewModel.deleteNotification("n1")
        advanceUntilIdle()

        coVerify { userContentDao.deleteNotification("n1") }
    }

    @Test
    fun `clearAll calls userContentDao clearNotifications`() = runTest(testDispatcher) {
        coEvery { userContentDao.clearNotifications() } returns 5
        val viewModel = NotificationsViewModel(userContentDao, sessionManager)

        viewModel.clearAll()
        advanceUntilIdle()

        coVerify { userContentDao.clearNotifications() }
    }
}
