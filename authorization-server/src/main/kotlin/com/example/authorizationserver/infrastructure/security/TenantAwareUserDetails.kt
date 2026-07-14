package com.example.authorizationserver.infrastructure.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class TenantAwareUserDetails(
    private val username: String,
    private val password: String,
    private val authorities: Collection<GrantedAuthority>,
    private val enabled: Boolean,
    private val accountNonLocked: Boolean,
    val tenantId: Long?,
    val userId: Long?
) : UserDetails {
    override fun getUsername(): String = username
    override fun getPassword(): String = password
    override fun getAuthorities(): Collection<GrantedAuthority> = authorities
    override fun isEnabled(): Boolean = enabled
    override fun isAccountNonExpired(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = accountNonLocked
}
