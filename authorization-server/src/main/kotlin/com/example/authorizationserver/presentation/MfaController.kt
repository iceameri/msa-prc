package com.example.authorizationserver.presentation

import com.example.authorizationserver.application.port.out.UserCachePort
import com.example.authorizationserver.application.service.LoginAttemptService
import com.example.authorizationserver.application.service.MfaService
import com.example.authorizationserver.application.service.UserDetailsServiceImpl
import com.example.authorizationserver.domain.user.UserRepository
import com.example.authorizationserver.presentation.dto.MfaCodeRequest
import com.example.authorizationserver.presentation.dto.MfaSetupResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.savedrequest.HttpSessionRequestCache
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/mfa")
class MfaController(
    private val mfaService: MfaService,
    private val userRepository: UserRepository,
    private val userDetailsService: UserDetailsServiceImpl,
    private val loginAttemptService: LoginAttemptService,
    private val userCachePort: UserCachePort
) {
    private val requestCache = HttpSessionRequestCache()

    @GetMapping("/setup")
    fun setup(session: HttpSession): ResponseEntity<MfaSetupResponse> {
        val username = session.getAttribute("MFA_SETUP_USER") as? String
            ?: return ResponseEntity.badRequest().build()
        val data = mfaService.generateSetup(username)
        return ResponseEntity.ok(MfaSetupResponse(data.qrCodeBase64))
    }

    @PostMapping("/enable")
    fun enable(@RequestBody req: MfaCodeRequest, session: HttpSession): ResponseEntity<Void> {
        val username = session.getAttribute("MFA_SETUP_USER") as? String
            ?: return ResponseEntity.badRequest().build()
        return if (mfaService.enableMfa(username, req.code)) ResponseEntity.ok().build()
        else ResponseEntity.badRequest().build()
    }

    @PostMapping("/verify")
    fun verify(
        @RequestParam code: String,
        request: HttpServletRequest,
        response: HttpServletResponse,
        session: HttpSession
    ) {
        val username = session.getAttribute("MFA_PENDING_USER") as? String
            ?: run { response.sendRedirect("/login"); return }

        val user = userRepository.findByUsername(username)
            ?: run { response.sendRedirect("/login"); return }

        if (!mfaService.verifyCode(user.mfaSecret!!, code)) {
            response.sendRedirect("/mfa/verify?error")
            return
        }

        session.removeAttribute("MFA_PENDING_USER")

        val userDetails = userDetailsService.loadUserByUsername(username)
        val auth = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
        SecurityContextHolder.getContext().authentication = auth

        loginAttemptService.onLoginSuccess(username)
        userCachePort.saveAuthorities(username, userDetails.authorities.map { it.authority }.toSet())

        val savedRequest = requestCache.getRequest(request, response)
        response.sendRedirect(savedRequest?.redirectUrl ?: "/")
    }

    @DeleteMapping("/disable")
    fun disable(session: HttpSession): ResponseEntity<Void> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).build()
        mfaService.disableMfa(username)
        return ResponseEntity.ok().build()
    }
}
