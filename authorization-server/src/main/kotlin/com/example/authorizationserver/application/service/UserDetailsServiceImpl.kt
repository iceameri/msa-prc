package com.example.authorizationserver.application.service

import com.example.authorizationserver.application.port.out.UserCachePort
import com.example.authorizationserver.domain.user.UserRepository
import com.example.authorizationserver.infrastructure.security.TenantAwareUserDetails
import com.example.authorizationserver.infrastructure.tenant.TenantContext
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class UserDetailsServiceImpl(
    private val userRepository: UserRepository,
    private val userCachePort: UserCachePort
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val tenantId = TenantContext.get()

        val user = if (tenantId != null) {
            userCachePort.getUser(username, tenantId)
                ?: userRepository.findByUsernameAndTenantId(username, tenantId)
                    ?.also { userCachePort.saveUser(it) }
        } else {
            userCachePort.getUser(username)
                ?: userRepository.findByUsername(username)
                    ?.also { userCachePort.saveUser(it) }
        } ?: throw UsernameNotFoundException(username)

        val isLocked = user.lockedUntil?.isAfter(Instant.now()) ?: false

        return TenantAwareUserDetails(
            username = user.username,
            password = user.password,
            authorities = user.authorities.map { SimpleGrantedAuthority(it) },
            enabled = user.enabled,
            accountNonLocked = !isLocked,
            tenantId = user.tenantId,
            userId = user.id
        )
    }
}
