package com.peakconnect.security

import com.peakconnect.entity.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

class CustomUserDetails(
    val id: UUID?,
    private val email: String,
    private val pass: String,
    private val authorities: Collection<GrantedAuthority>
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> = authorities
    override fun getPassword(): String = pass
    override fun getUsername(): String = email

    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = true

    companion object {
        fun build(user: User): CustomUserDetails {
            val authorities = listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
            return CustomUserDetails(
                id = user.id,
                email = user.email,
                pass = user.password,
                authorities = authorities
            )
        }
    }
}
