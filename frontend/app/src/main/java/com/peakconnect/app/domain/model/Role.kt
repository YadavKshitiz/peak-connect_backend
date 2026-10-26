package com.peakconnect.app.domain.model

import com.peakconnect.app.core.network.RoleSerializer
import kotlinx.serialization.Serializable

@Serializable(with = RoleSerializer::class)
enum class Role {
    TREKKER,
    GUIDE,
    ADMIN
}
