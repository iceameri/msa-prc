package com.example.authorizationserver.application.service

import com.example.authorizationserver.application.port.out.UserCachePort
import com.example.authorizationserver.domain.user.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
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
        val user = userCachePort.getUser(username)
            ?: userRepository.findByUsername(username)
                ?.also { userCachePort.saveUser(it) }
            ?: throw UsernameNotFoundException(username)

        val isLocked = user.lockedUntil?.isAfter(Instant.now()) ?: false

        return User.builder()
            .username(user.username)
            .password(user.password)
            .authorities(user.authorities.map { SimpleGrantedAuthority(it) })
            .disabled(!user.enabled)
            .accountLocked(isLocked)
            .build()
    }
}
