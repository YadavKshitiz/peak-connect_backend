package com.peakconnect.app.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakconnect.app.core.session.SessionManager
import com.peakconnect.app.core.session.SessionState
import com.peakconnect.app.data.local.dao.UserContentDao
import com.peakconnect.app.data.local.entity.NotificationEntity
import com.peakconnect.app.domain.model.Role
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val notifications: List<NotificationEntity> = emptyList(),
    val isLoading: Boolean = false,
    val userRole: Role? = null
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val userContentDao: UserContentDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        observeNotifications()
        observeRole()
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            userContentDao.getNotificationsFlow().collectLatest { list ->
                _uiState.update { it.copy(notifications = list) }
            }
        }
    }

    private fun observeRole() {
        val session = sessionManager.sessionState.value
        if (session is SessionState.LoggedIn) {
            _uiState.update { it.copy(userRole = session.role) }
        }
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            userContentDao.markNotificationAsRead(id)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            userContentDao.markAllNotificationsAsRead()
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            userContentDao.deleteNotification(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            userContentDao.clearNotifications()
        }
    }
}
